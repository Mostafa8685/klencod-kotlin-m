package com.apk3rade.mangoide

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class TemplatesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)

        val prefs = AppPrefs(this)
        val pm = ProjectManager(this)
        val container: LinearLayout = findViewById(R.id.containerTemplates)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        for (tpl in ProjectTemplates.all()) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_rounded)
                setPadding(32, 28, 32, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
            }
            val title = TextView(this).apply {
                text = tpl.title
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val desc = TextView(this).apply {
                text = tpl.description
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 8, 0, 16)
            }
            val btn = Button(this).apply {
                text = "إنشاء مشروع من هذا القالب"
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_btn_primary)
                setTextColor(0xFF0B1120.toInt())
                setOnClickListener {
                    val input = EditText(this@TemplatesActivity).apply {
                        setText(tpl.defaultProjectName)
                    }
                    AlertDialog.Builder(this@TemplatesActivity)
                        .setTitle("اسم المشروع الجديد")
                        .setView(input)
                        .setPositiveButton("إنشاء") { _, _ ->
                            val created = pm.createFromTemplate(tpl, input.text.toString())
                            prefs.activeProject = created.name
                            Toast.makeText(this@TemplatesActivity, "تم إنشاء ${created.name}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                }
            }
            card.addView(title)
            card.addView(desc)
            card.addView(btn)
            container.addView(card)
        }
    }
}
