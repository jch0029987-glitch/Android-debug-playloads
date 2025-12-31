package com.jch.debugclient

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    // Termux FastAPI server on same device
    private const val BASE_URL = "http://127.0.0.1:8000"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)  // Longer for payload execution
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Test FastAPI server connection
     */
    fun testConnection(callback: (Boolean, String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/docs")  // FastAPI docs endpoint
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "FastAPI server not reachable: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val isFastAPI = response.header("Server")?.contains("uvicorn") == true
                    val message = if (isFastAPI) {
                        "✅ FastAPI server is running!"
                    } else {
                        "⚠️ Server responded but might not be FastAPI"
                    }
                    callback(response.isSuccessful, "$message (HTTP ${response.code})")
                }
            }
        })
    }

    /**
     * List available payloads from FastAPI
     */
    fun listPayloads(callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/payloads")
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("ERROR: Failed to list payloads: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string() ?: "{}"
                    try {
                        val json = JSONObject(body)
                        val formatted = formatPayloadList(json)
                        callback(formatted)
                    } catch (e: Exception) {
                        callback("Response: $body")
                    }
                }
            }
        })
    }

    /**
     * Execute a payload with optional arguments
     */
    fun executePayload(
        payloadName: String,
        args: Map<String, Any> = emptyMap(),
        async: Boolean = false,
        callback: (String) -> Unit
    ) {
        // Build request body
        val requestBody = JSONObject().apply {
            put("name", payloadName)
            put("args", JSONObject(args))
            put("async", async)
            put("timestamp", System.currentTimeMillis())
        }
        
        val body = requestBody.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/payloads/execute")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("""
                    ❌ Connection Failed
                    Error: ${e.message}
                    
                    Make sure:
                    1. Termux is running
                    2. FastAPI server is started:
                       uvicorn main:app --host 0.0.0.0 --port 8000
                    3. Port 8000 is accessible
                """.trimIndent())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyText = response.body?.string() ?: "{}"
                    try {
                        val json = JSONObject(bodyText)
                        val formatted = formatResponse(json, response.code)
                        callback(formatted)
                    } catch (e: Exception) {
                        callback("""
                            📄 Raw Response (HTTP ${response.code}):
                            $bodyText
                        """.trimIndent())
                    }
                }
            }
        })
    }

    /**
     * Check payload execution status (for async jobs)
     */
    fun checkStatus(jobId: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/payloads/status/$jobId")
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("ERROR checking status: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string() ?: "{}"
                    callback(formatResponse(JSONObject(body), response.code))
                }
            }
        })
    }

    /**
     * Get payload documentation
     */
    fun getPayloadInfo(payloadName: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/payloads/info/$payloadName")
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("ERROR: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string() ?: "{}"
                    callback(formatPayloadInfo(JSONObject(body)))
                }
            }
        })
    }

    // Helper functions
    private fun formatPayloadList(json: JSONObject): String {
        return try {
            val payloads = json.getJSONArray("payloads")
            val sb = StringBuilder("📦 Available Payloads:\n\n")
            
            for (i in 0 until payloads.length()) {
                val payload = payloads.getJSONObject(i)
                sb.append("• ${payload.getString("name")}\n")
                sb.append("  📝 ${payload.getString("description")}\n")
                if (payload.has("author")) {
                    sb.append("  👤 ${payload.getString("author")}\n")
                }
                sb.append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            "📄 Response:\n${json.toString(2)}"
        }
    }
    
    private fun formatResponse(json: JSONObject, statusCode: Int): String {
        return try {
            val sb = StringBuilder()
            
            if (statusCode >= 200 && statusCode < 300) {
                sb.append("✅ Success\n\n")
            } else {
                sb.append("❌ Error (HTTP $statusCode)\n\n")
            }
            
            // Add common fields
            if (json.has("status")) {
                sb.append("Status: ${json.getString("status")}\n")
            }
            if (json.has("job_id")) {
                sb.append("Job ID: ${json.getString("job_id")}\n")
            }
            if (json.has("message")) {
                sb.append("Message: ${json.getString("message")}\n")
            }
            if (json.has("result")) {
                sb.append("\n📊 Result:\n${json.getString("result")}\n")
            }
            if (json.has("error")) {
                sb.append("\n🔥 Error:\n${json.getString("error")}\n")
            }
            if (json.has("output")) {
                sb.append("\n📋 Output:\n${json.getString("output")}\n")
            }
            
            // If nothing else, show raw JSON
            if (sb.length <= 10) {
                sb.append("\n📄 Raw:\n${json.toString(2)}")
            }
            
            sb.toString()
        } catch (e: Exception) {
            "📄 Raw JSON:\n${json.toString(2)}"
        }
    }
    
    private fun formatPayloadInfo(json: JSONObject): String {
        return try {
            val sb = StringBuilder("📄 Payload Information\n\n")
            
            sb.append("Name: ${json.getString("name")}\n")
            sb.append("Description: ${json.getString("description")}\n")
            
            if (json.has("author")) {
                sb.append("Author: ${json.getString("author")}\n")
            }
            if (json.has("version")) {
                sb.append("Version: ${json.getString("version")}\n")
            }
            if (json.has("parameters")) {
                val params = json.getJSONObject("parameters")
                sb.append("\n⚙️ Parameters:\n")
                for (key in params.keys()) {
                    val param = params.getJSONObject(key)
                    sb.append("  • $key: ${param.getString("type")}")
                    if (param.has("default")) {
                        sb.append(" (default: ${param.get("default")})")
                    }
                    if (param.has("description")) {
                        sb.append(" - ${param.getString("description")}")
                    }
                    sb.append("\n")
                }
            }
            
            sb.toString()
        } catch (e: Exception) {
            "📄 Raw:\n${json.toString(2)}"
        }
    }
}
