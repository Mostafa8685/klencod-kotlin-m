package com.apk3rade.mangoide

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CloudBuildActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var projectManager: ProjectManager
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_build)

        prefs = AppPrefs(this)
        projectManager = ProjectManager(this)
        tvLog = findViewById(R.id.tvBuildLog)

        val etToken: EditText = findViewById(R.id.etGithubToken)
        val etRepo: EditText = findViewById(R.id.etGithubRepo)
        val etBranch: EditText = findViewById(R.id.etGithubBranch)

        etToken.setText(prefs.githubToken)
        etRepo.setText(prefs.githubRepo)
        etBranch.setText(prefs.githubBranch)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnStartCloudBuild).setOnClickListener {
            val token = etToken.text.toString().trim()
            val repo = etRepo.text.toString().trim()
            val branch = etBranch.text.toString().trim().ifEmpty { "main" }
            if (token.isEmpty() || !repo.contains("/")) {
                tvLog.text = "يرجى إدخال رمز GitHub واسم المستودع بصيغة owner/repo."
                return@setOnClickListener
            }
            prefs.githubToken = token
            prefs.githubRepo = repo
            prefs.githubBranch = branch

            val projectName = intent.getStringExtra("project_name").orEmpty().ifEmpty { prefs.activeProject }
            val projectDir = projectManager.getProjectDir(projectName)
            tvLog.text = "جارٍ مزامنة المشروع ${projectDir.name} مع GitHub..."

            CoroutineScope(Dispatchers.IO).launch {
                val client = GitHubCloudClient(token, repo, branch)
                val ok = client.syncProject(projectDir) { line ->
                    runOnUiThread { tvLog.append("\n$line") }
                }
                val summary = if (ok) {
                    "\nاكتمل رفع الملفات. تم تشغيل GitHub Actions تلقائياً.\n" + client.fetchLatestRunSummary()
                } else {
                    "\nتوقفت عملية الرفع بسبب خطأ."
                }
                withContext(Dispatchers.Main) { tvLog.append(summary) }
            }
        }

        findViewById<Button>(R.id.btnCheckBuildStatus).setOnClickListener {
            val token = etToken.text.toString().trim()
            val repo = etRepo.text.toString().trim()
            val branch = etBranch.text.toString().trim().ifEmpty { "main" }
            if (token.isEmpty() || !repo.contains("/")) {
                tvLog.text = "أدخل بيانات المستودع أولاً."
                return@setOnClickListener
            }
            tvLog.text = "جارٍ التحقق من حالة آخر بناء سحابي..."
            CoroutineScope(Dispatchers.IO).launch {
                val status = GitHubCloudClient(token, repo, branch).fetchLatestRunSummary()
                withContext(Dispatchers.Main) { tvLog.text = status }
            }
        }
    }
}
