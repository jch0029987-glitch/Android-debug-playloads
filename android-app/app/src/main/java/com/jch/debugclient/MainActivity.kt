package com.jch.debugclient

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val payloadInput = findViewById<EditText>(R.id.payloadName)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val outputView = findViewById<TextView>(R.id.output)

        sendButton.setOnClickListener {
            val payloadName = payloadInput.text.toString()

            if (payloadName.isBlank()) {
                outputView.text = "Enter payload name"
                return@setOnClickListener
            }

            outputView.text = "Sending payload..."

            ApiClient.sendPayload(payloadName) { result ->
                runOnUiThread {
                    outputView.text = result
                }
            }
        }
    }
}
