package com.jch.debugclient

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import okhttp3.*

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var payloadInput: EditText
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Catch all uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            logEvent("CRASH:\n${sw}", this)
            runOnUiThread {
                Toast.makeText(this, "App crashed! Check crash log", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(R.layout.activity_main)

        outputView = findViewById(R.id.outputView)
        payloadInput = findViewById(R.id.payloadInput)

        val sendButton = findViewById<Button>(R.id.sendPayload)
        val viewCrashButton = findViewById<Button>(R.id.viewCrashLog)

        // Send payload button
        sendButton.setOnClickListener {
            val payloadName = payloadInput.text.toString()
            outputView.text = "Sending payload..."
            sendPayload(payloadName)
        }

        // View crash log button
        viewCrashButton.setOnClickListener {
            val crashFile = File(filesDir, "crash_log.txt")
            outputView.text = if (crashFile.exists()) crashFile.readText() else "No logs yet"
        }
    }

    // Append events to crash_log.txt
    private fun logEvent(message: String, context: MainActivity) {
        try {
            val crashFile = File(context.filesDir, "crash_log.txt")
            crashFile.appendText("${System.currentTimeMillis()}: $message\n")
        } catch (_: Exception) {
            // Fail silently
        }
    }

    // Send payload asynchronously
    private fun sendPayload(payloadName: String) {
        val requestBody = FormBody.Builder()
            .add("payload", payloadName)
            .build()

        val request = Request.Builder()
            .url("http://127.0.0.1:8080/payloads/run") // Termux server URL
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logEvent("NETWORK ERROR: ${e.message}", this@MainActivity)
                runOnUiThread {
                    outputView.text = "Server not reachable: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val respText = response.body?.string() ?: "Empty response"
                logEvent("Payload '$payloadName' sent, response: $respText", this@MainActivity)
                runOnUiThread {
                    outputView.text = respText
                }
            }
        })
    }
}
