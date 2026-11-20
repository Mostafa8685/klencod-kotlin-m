package com.apk3rade.mangoide

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {
    fun ask(prompt: String, codeContext: String?): String {
        if (apiKey.isBlank()) {
            return "يرجى إدخال مفتاح API من شاشة الإعدادات أولاً."
        }
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "أنت مساعد برمجة محترف داخل تطبيق Mango IDE التابع لموقع Apk3rade. أجب بالعربية بوضوح واختصار.")
            })
            val fullUser = if (!codeContext.isNullOrBlank()) {
                "$prompt\n\nالكود الحالي:\n```\n${codeContext.take(6000)}\n```"
            } else prompt
            put(JSONObject().apply {
                put("role", "user")
                put("content", fullUser)
            })
        }
        val body = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o-mini" })
            put("messages", messages)
        }.toString()

        val conn = (URL(baseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) {
            return "خطأ من مزود الذكاء الاصطناعي ($code): ${resp.take(250)}"
        }
        val json = JSONObject(resp)
        val choices = json.optJSONArray("choices")
        return choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            ?: "لم يتم استلام رد نصي من النموذج."
    }
}
