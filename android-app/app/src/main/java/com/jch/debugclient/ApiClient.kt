package com.jch.debugclient

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ApiClient {

    // CHANGE THIS when not running locally
    private const val BASE_URL = "http://127.0.0.1:8000"

    private val client = OkHttpClient()

    fun sendPayload(
        payloadName: String,
        callback: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("name", payloadName)
            put("args", JSONObject())
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/payloads/run")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("ERROR: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.body?.string() ?: "No response")
            }
        })
    }
}
