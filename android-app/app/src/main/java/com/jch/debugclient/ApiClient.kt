package com.jch.debugclient

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    // FastAPI server (Termux / same device)
    private const val BASE_URL = "http://127.0.0.1:8000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Test server connection
     */
    fun testConnection(callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("❌ Server not reachable: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                callback("✅ Server OK (HTTP ${response.code})")
            }
        })
    }

    /**
     * Get available payloads
     * Server returns a LIST, not JSON object
     */
    fun listPayloads(callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/payloads")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("❌ Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string() ?: "[]"
                    try {
                        val array = JSONArray(body)
                        val sb = StringBuilder("📦 Payloads:\n\n")
                        for (i in 0 until array.length()) {
                            sb.append("• ").append(array.getString(i)).append("\n")
                        }
                        callback(sb.toString())
                    } catch (e: Exception) {
                        callback("📄 Raw response:\n$body")
                    }
                }
            }
        })
    }

    /**
     * Execute a payload
     * THIS MATCHES FastAPI EXACTLY
     */
    fun executePayload(
        payloadName: String,
        args: Map<String, Any> = emptyMap(),
        callback: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("payload", payloadName)
            put("args", JSONObject(args))
        }

        val body = json
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/payloads/execute")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("❌ Request failed:\n${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyText = response.body?.string() ?: "{}"
                    callback(formatResponse(bodyText, response.code))
                }
            }
        })
    }

    /**
     * Pretty response formatting
     */
    private fun formatResponse(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val sb = StringBuilder()

            if (code in 200..299) {
                sb.append("✅ Success\n\n")
            } else {
                sb.append("❌ Error (HTTP $code)\n\n")
            }

            if (json.has("output")) {
                sb.append("Output:\n").append(json.get("output"))
            } else if (json.has("error")) {
                sb.append("Error:\n").append(json.get("error"))
            } else {
                sb.append("Response:\n").append(json.toString(2))
            }

            sb.toString()
        } catch (e: Exception) {
            "HTTP $code\n$body"
        }
    }
}
