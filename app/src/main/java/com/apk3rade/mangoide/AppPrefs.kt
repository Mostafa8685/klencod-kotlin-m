package com.apk3rade.mangoide

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("mango_ide_prefs", Context.MODE_PRIVATE)

    var activeProject: String
        get() = prefs.getString("active_project", "") ?: ""
        set(value) = prefs.edit().putString("active_project", value).apply()

    var fontSize: Int
        get() = prefs.getInt("font_size", 14)
        set(value) = prefs.edit().putInt("font_size", value.coerceIn(10, 26)).apply()

    var githubToken: String
        get() = prefs.getString("github_token", "") ?: ""
        set(value) = prefs.edit().putString("github_token", value.trim()).apply()

    var githubRepo: String
        get() = prefs.getString("github_repo", "") ?: ""
        set(value) = prefs.edit().putString("github_repo", value.trim()).apply()

    var githubBranch: String
        get() = prefs.getString("github_branch", "main") ?: "main"
        set(value) = prefs.edit().putString("github_branch", value.trim().ifEmpty { "main" }).apply()

    var aiBaseUrl: String
        get() = prefs.getString("ai_base_url", "https://api.openai.com/v1/chat/completions")
            ?: "https://api.openai.com/v1/chat/completions"
        set(value) = prefs.edit().putString("ai_base_url", value.trim()).apply()

    var aiApiKey: String
        get() = prefs.getString("ai_api_key", "") ?: ""
        set(value) = prefs.edit().putString("ai_api_key", value.trim()).apply()

    var aiModel: String
        get() = prefs.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString("ai_model", value.trim()).apply()
}
