package com.jch.debugclient

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var payloadInput: EditText
    private lateinit var argsInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputView = findViewById(R.id.outputView)
        payloadInput = findViewById(R.id.payloadInput)
        argsInput = findViewById(R.id.argsInput)

        outputView.movementMethod = ScrollingMovementMethod()

        findViewById<Button>(R.id.testConnection).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.listPayloads).setOnClickListener {
            listPayloads()
        }

        findViewById<Button>(R.id.sendPayload).setOnClickListener {
            executePayload()
        }

        testConnection()
    }

    private fun testConnection() {
        show("🔌 Testing server...")
        ApiClient.testConnection { ok, msg ->
            runOnUiThread {
                show(if (ok) "✅ $msg" else "❌ $msg")
            }
        }
    }

    private fun listPayloads() {
        show("📦 Fetching payloads...")
        ApiClient.listPayloads { result ->
            runOnUiThread {
                show(result)
            }
        }
    }

    private fun executePayload() {
        val payload = payloadInput.text.toString().trim()
        if (payload.isEmpty()) {
            show("⚠️ Payload name required")
            return
        }

        val args = parseArgs(argsInput.text.toString())

        show("🚀 Running payload: $payload\nArgs: $args")

        ApiClient.executePayload(payload, args) { result ->
            runOnUiThread {
                show(result)
            }
        }
    }

    private fun parseArgs(input: String): Map<String, Any> {
        if (input.isBlank()) return emptyMap()

        return try {
            val json = JSONObject(input)
            val map = mutableMapOf<String, Any>()
            for (key in json.keys()) {
                map[key] = json.get(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun show(text: String) {
        outputView.text = text
        outputView.post {
            val scroll =
                outputView.layout.getLineTop(outputView.lineCount) - outputView.height
            outputView.scrollTo(0, maxOf(scroll, 0))
        }
    }
}
