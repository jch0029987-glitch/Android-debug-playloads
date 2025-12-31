package com.jch.debugclient

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var payloadInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val crashFile = File(filesDir, "crash_log.txt")
            crashFile.writeText(stackTrace)

            runOnUiThread {
                Toast.makeText(this, "App crashed! Check crash log", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(R.layout.activity_main)

        outputView = findViewById(R.id.outputView)
        payloadInput = findViewById(R.id.payloadInput)

        val sendButton = findViewById<Button>(R.id.sendPayload)
        val viewCrashButton = findViewById<Button>(R.id.viewCrashLog)

        sendButton.setOnClickListener {
            val payloadName = payloadInput.text.toString()
            try {
                ApiClient.sendPayload(payloadName) { result ->
                    runOnUiThread { outputView.text = result }
                }
            } catch (e: Exception) {
                outputView.text = "ERROR: ${e.message}"
                val crashFile = File(filesDir, "crash_log.txt")
                crashFile.appendText("\nNETWORK ERROR: ${e.message}\n")
            }
        }

        viewCrashButton.setOnClickListener {
            val crashFile = File(filesDir, "crash_log.txt")
            if (crashFile.exists()) {
                outputView.text = crashFile.readText()
            } else {
                outputView.text = "No crash log found"
            }
        }
    }
}
