package com.apk3rade.mangoide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = AppPrefs(this)
        val etFontSize: EditText = findViewById(R.id.etFontSize)
        val etBaseUrl: EditText = findViewById(R.id.etAiBaseUrl)
        val etApiKey: EditText = findViewById(R.id.etAiApiKey)
        val etModel: EditText = findViewById(R.id.etAiModel)

        etFontSize.setText(prefs.fontSize.toString())
        etBaseUrl.setText(prefs.aiBaseUrl)
        etApiKey.setText(prefs.aiApiKey)
        etModel.setText(prefs.aiModel)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnOpenTelegram).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram_url)))
            runCatching { startActivity(intent) }.onFailure {
                Toast.makeText(this, "قناة التليجرام: @apk3rade", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            prefs.fontSize = etFontSize.text.toString().toIntOrNull() ?: 14
            prefs.aiBaseUrl = etBaseUrl.text.toString().ifBlank { "https://api.openai.com/v1/chat/completions" }
            prefs.aiApiKey = etApiKey.text.toString()
            prefs.aiModel = etModel.text.toString().ifBlank { "gpt-4o-mini" }
            Toast.makeText(this, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
