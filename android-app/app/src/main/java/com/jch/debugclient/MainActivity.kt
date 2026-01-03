package com.jch.debugclient

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var payloadInput: EditText
    private lateinit var argsInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupCrashHandler()
        setContentView(R.layout.activity_main)

        outputView = findViewById(R.id.outputView)
        payloadInput = findViewById(R.id.payloadInput)
        argsInput = findViewById(R.id.argsInput)

        outputView.movementMethod = ScrollingMovementMethod()

        findViewById<Button>(R.id.sendPayload).setOnClickListener {
            executePayload()
        }

        findViewById<Button>(R.id.testConnection).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.listPayloads).setOnClickListener {
            listPayloads()
        }

        findViewById<Button>(R.id.viewCrashLog).setOnClickListener {
            viewCrashLog()
        }

        findViewById<Button>(R.id.clearLogs).setOnClickListener {
            clearLogs()
        }

        testConnection()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "crash_log.txt")
                    .appendText("\n=== CRASH ${Date()} ===\n$sw\n")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun testConnection() {
        showOutput("🔌 Testing FastAPI connection...")
        ApiClient.testConnection { success, message ->
            runOnUiThread {
                showOutput(message)
                if (success) listPayloads()
            }
        }
    }

    private fun listPayloads() {
        showOutput("📦 Fetching payload list...")
        ApiClient.listPayloads { result ->
            runOnUiThread {
                showOutput(result)
            }
        }
    }

    private fun executePayload() {
        val payloadName = payloadInput.text.toString().trim()
        if (payloadName.isEmpty()) {
            showOutput("⚠️ Enter a payload name")
            return
        }

        val args = parseArgs(argsInput.text.toString())

        showOutput("🚀 Executing payload: $payloadName")

        ApiClient.executePayload(payloadName, args) { result ->
            runOnUiThread {
                showOutput(result)
            }
        }
    }

    private fun parseArgs(text: String): Map<String, Any> {
        if (text.isBlank()) return emptyMap()

        return try {
            val json = org.json.JSONObject(text)
            val map = mutableMapOf<String, Any>()
            for (key in json.keys()) {
                map[key] = json.get(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun viewCrashLog() {
        val file = File(filesDir, "crash_log.txt")
        showOutput(
            if (file.exists() && file.length() > 0)
                file.readText()
            else
                "📭 No crash logs"
        )
    }

    private fun clearLogs() {
        File(filesDir, "crash_log.txt").writeText("")
        showOutput("🧹 Logs cleared")
    }

    private fun showOutput(text: String) {
        outputView.text = text
        outputView.post {
            val scroll =
                outputView.layout.getLineTop(outputView.lineCount) - outputView.height
            outputView.scrollTo(0, maxOf(scroll, 0))
        }
    }
}
