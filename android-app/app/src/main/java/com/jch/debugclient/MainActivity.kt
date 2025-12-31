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
        
        // Initialize views
        outputView = findViewById(R.id.outputView)
        payloadInput = findViewById(R.id.payloadInput)
        argsInput = findViewById(R.id.argsInput)
        
        // Make output scrollable
        outputView.movementMethod = ScrollingMovementMethod()
        
        // Setup buttons
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
        
        // Auto-test connection on startup
        testConnection()
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.appendText("\n=== CRASH ${Date()} ===\n${sw.toString()}\n\n")
            } catch (_: Exception) {}
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun testConnection() {
        showOutput("🔌 Testing FastAPI connection...")
        ApiClient.testConnection { success, message ->
            runOnUiThread {
                if (success) {
                    showOutput("✅ $message")
                    // Auto-list payloads after successful connection
                    listPayloads()
                } else {
                    showOutput("""
                        ❌ $message
                        
                        Start Termux and run:
                        cd ~/your-fastapi-project
                        uvicorn main:app --host 0.0.0.0 --port 8000
                        
                        Then refresh connection.
                    """.trimIndent())
                }
            }
        }
    }
    
    private fun listPayloads() {
        showOutput("📦 Loading available payloads...")
        ApiClient.listPayloads { result ->
            runOnUiThread {
                showOutput(result)
                logEvent("Listed payloads")
            }
        }
    }
    
    private fun executePayload() {
        val payloadName = payloadInput.text.toString().trim()
        if (payloadName.isEmpty()) {
            showOutput("⚠️ Please enter a payload name")
            return
        }
        
        val argsText = argsInput.text.toString().trim()
        val args = parseArgs(argsText)
        
        showOutput("🚀 Executing: $payloadName")
        if (args.isNotEmpty()) {
            showOutput("⚙️ Args: $args")
        }
        
        ApiClient.executePayload(payloadName, args) { result ->
            runOnUiThread {
                showOutput(result)
                logEvent("Executed payload: $payloadName\nResult: $result")
            }
        }
    }
    
    private fun parseArgs(argsText: String): Map<String, Any> {
        val args = mutableMapOf<String, Any>()
        if (argsText.isBlank()) return args
        
        try {
            // Try parsing as JSON
            if (argsText.trim().startsWith("{")) {
                val json = org.json.JSONObject(argsText)
                for (key in json.keys()) {
                    args[key] = json.get(key)
                }
            } else {
                // Parse as key=value pairs
                argsText.split(",").forEach { pair ->
                    val parts = pair.trim().split("=")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        
                        // Try to parse as number
                        when {
                            value.toIntOrNull() != null -> args[key] = value.toInt()
                            value.toDoubleOrNull() != null -> args[key] = value.toDouble()
                            value == "true" || value == "false" -> args[key] = value.toBoolean()
                            else -> args[key] = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showOutput("⚠️ Could not parse args: ${e.message}\nUsing empty args.")
        }
        
        return args
    }
    
    private fun viewCrashLog() {
        try {
            val crashFile = File(filesDir, "crash_log.txt")
            val text = if (crashFile.exists() && crashFile.length() > 0) {
                crashFile.readText()
            } else {
                "📭 No crash logs found."
            }
            showOutput("📋 Crash Log:\n\n$text")
        } catch (e: Exception) {
            showOutput("❌ Error reading logs: ${e.message}")
        }
    }
    
    private fun clearLogs() {
        try {
            val crashFile = File(filesDir, "crash_log.txt")
            if (crashFile.exists()) {
                crashFile.writeText("")  // Clear file
                showOutput("🧹 Logs cleared")
            } else {
                showOutput("📭 No logs to clear")
            }
        } catch (e: Exception) {
            showOutput("❌ Error clearing logs: ${e.message}")
        }
    }
    
    private fun showOutput(text: String) {
        outputView.text = text
        // Auto-scroll to bottom
        outputView.post {
            val scrollAmount = outputView.layout.getLineTop(outputView.lineCount) - outputView.height
            if (scrollAmount > 0) {
                outputView.scrollTo(0, scrollAmount)
            } else {
                outputView.scrollTo(0, 0)
            }
        }
    }
    
    private fun logEvent(message: String) {
        try {
            val logFile = File(filesDir, "activity_log.txt")
            logFile.appendText("${Date()}: $message\n\n")
        } catch (_: Exception) {}
    }
}
