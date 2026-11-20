package com.apk3rade.mangoide

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import java.util.concurrent.Executors

class CloudBuildActivity : Activity() {

    private lateinit var prefs: AppPrefs
    private lateinit var projectManager: ProjectManager
    private lateinit var tvLog: TextView
    private val executor = Executors.newSingleThreadExecutor()

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

            executor.execute {
                val client = GitHubCloudClient(token, repo, branch)
                val ok = client.syncProject(projectDir) { line ->
                    runOnUiThread { tvLog.append("\n$line") }
                }
                val summary = if (ok) {
                    "\nاكتمل رفع الملفات. تم تشغيل GitHub Actions تلقائياً.\n" + client.fetchLatestRunSummary()
                } else {
                    "\nتوقفت عملية الرفع بسبب خطأ."
                }
                runOnUiThread { tvLog.append(summary) }
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
            executor.execute {
                val status = GitHubCloudClient(token, repo, branch).fetchLatestRunSummary()
                runOnUiThread { tvLog.text = status }
            }
        }
    }
}
