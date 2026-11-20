package com.apk3rade.mangoide

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class GitHubCloudClient(
    private val token: String,
    private val repo: String,
    private val branch: String
) {
    private val apiBase = "https://api.github.com/repos/$repo"

    private fun request(method: String, path: String, body: String? = null): Pair<Int, String> {
        val conn = (URL("$apiBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 25000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to text
    }

    fun uploadFile(relativePath: String, contentBytes: ByteArray): Pair<Boolean, String> {
        val cleanPath = relativePath.trimStart('/')
        val (getCode, getBody) = request("GET", "/contents/$cleanPath?ref=$branch")
        val existingSha = if (getCode == 200) JSONObject(getBody).optString("sha", "") else ""

        val payload = JSONObject().apply {
            put("message", "Mango IDE cloud sync: $cleanPath")
            put("content", Base64.encodeToString(contentBytes, Base64.NO_WRAP))
            put("branch", branch)
            if (existingSha.isNotEmpty()) put("sha", existingSha)
        }
        val (putCode, putBody) = request("PUT", "/contents/$cleanPath", payload.toString())
        return if (putCode in 200..201) {
            true to "تم رفع $cleanPath"
        } else {
            false to "فشل رفع $cleanPath ($putCode): ${putBody.take(180)}"
        }
    }

    fun syncProject(projectDir: File, onLog: (String) -> Unit): Boolean {
        val files = projectDir.walkTopDown().filter { it.isFile && !it.path.contains("/.git/") }.toList()
        if (files.isEmpty()) {
            onLog("لا توجد ملفات في المشروع للرفع.")
            return false
        }
        for (file in files) {
            val rel = file.relativeTo(projectDir).path.replace("\\", "/")
            val (ok, msg) = uploadFile(rel, file.readBytes())
            onLog(msg)
            if (!ok) return false
        }
        return true
    }

    fun fetchLatestRunSummary(): String {
        val (code, body) = request("GET", "/actions/runs?per_page=1")
        if (code != 200) return "تعذر جلب حالة البناء ($code): ${body.take(200)}"
        val runs = JSONObject(body).optJSONArray("workflow_runs")
        if (runs == null || runs.length() == 0) {
            return "لا توجد عمليات بناء مسجلة حتى الآن على هذا المستودع."
        }
        val run = runs.getJSONObject(0)
        val name = run.optString("name", "Workflow")
        val status = run.optString("status", "unknown")
        val conclusion = run.optString("conclusion", "قيد التنفيذ")
        val htmlUrl = run.optString("html_url", "")
        return "آخر بناء: $name\nالحالة: $status\nالنتيجة: $conclusion\nالرابط: $htmlUrl"
    }
}
