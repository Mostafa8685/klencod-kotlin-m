package com.apk3rade.mangoide

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class AiAssistantActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        val prefs = AppPrefs(this)
        val activeCode = intent.getStringExtra("active_code") ?: ""

        val tvHistory: TextView = findViewById(R.id.tvChatHistory)
        val scrollChat: ScrollView = findViewById(R.id.scrollChat)
        val etPrompt: EditText = findViewById(R.id.etPrompt)
        val cbInclude: CheckBox = findViewById(R.id.cbIncludeCode)
        val btnSend: Button = findViewById(R.id.btnSendPrompt)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener
            etPrompt.setText("")
            btnSend.isEnabled = false
            tvHistory.append("\n\nأنت: $prompt\nالمساعد: جارٍ التفكير...")
            scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }

            val ctx = if (cbInclude.isChecked) activeCode else null
            executor.execute {
                val reply = runCatching {
                    AiClient(prefs.aiBaseUrl, prefs.aiApiKey, prefs.aiModel).ask(prompt, ctx)
                }.getOrElse { "حدث خطأ في الاتصال: ${it.message}" }

                runOnUiThread {
                    tvHistory.append("\n$reply")
                    btnSend.isEnabled = true
                    scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        }
    }
}
