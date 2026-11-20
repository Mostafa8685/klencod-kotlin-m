package com.apk3rade.mangoide

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {

    private lateinit var prefs: AppPrefs
    private lateinit var projectManager: ProjectManager
    private lateinit var drawerOverlay: FrameLayout
    private lateinit var webView: WebView
    private lateinit var tvProjectTitle: TextView
    private lateinit var tvActiveFile: TextView
    private lateinit var tvStatus: TextView
    private lateinit var adapter: FileTreeAdapter

    private var currentProject: File? = null
    private var currentFile: File? = null
    private var latestContent: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppPrefs(this)
        projectManager = ProjectManager(this)

        drawerOverlay = findViewById(R.id.drawerOverlay)
        webView = findViewById(R.id.editorWebView)
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvActiveFile = findViewById(R.id.tvActiveFile)
        tvStatus = findViewById(R.id.tvStatus)

        val lvTree: ListView = findViewById(R.id.lvFileTree)
        adapter = FileTreeAdapter(this)
        lvTree.adapter = adapter
        lvTree.setOnItemClickListener { _, _, position, _ ->
            val node = adapter.getItem(position)
            if (!node.isDirectory) {
                openFile(node.file)
                drawerOverlay.visibility = View.GONE
            }
        }
        lvTree.setOnItemLongClickListener { _, _, position, _ ->
            showNodeActions(adapter.getItem(position))
            true
        }

        drawerOverlay.setOnClickListener { drawerOverlay.visibility = View.GONE }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(EditorBridge(), "MangoBridge")
        webView.loadUrl("file:///android_asset/editor/index.html")

        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerOverlay.visibility = View.VISIBLE
        }
        findViewById<ImageButton>(R.id.btnSave).setOnClickListener { saveCurrentFile(true) }
        findViewById<ImageButton>(R.id.btnPreview).setOnClickListener { previewHtmlIfPossible() }
        findViewById<ImageButton>(R.id.btnCloudBuild).setOnClickListener {
            saveCurrentFile(false)
            val intent = Intent(this, CloudBuildActivity::class.java)
            intent.putExtra("project_name", currentProject?.name ?: "")
            startActivity(intent)
        }
        findViewById<ImageButton>(R.id.btnAi).setOnClickListener {
            val intent = Intent(this, AiAssistantActivity::class.java)
            intent.putExtra("active_code", latestContent)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnSwitchProject).setOnClickListener { showProjectSwitcher() }
        findViewById<Button>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnNewFile).setOnClickListener { showCreateFileDialog() }
        findViewById<ImageButton>(R.id.btnExportZip).setOnClickListener { exportCurrentProjectZip() }
        findViewById<LinearLayout>(R.id.cardTelegramDrawer).setOnClickListener { openTelegramChannel() }
        findViewById<TextView>(R.id.tvChannelQuick).setOnClickListener { openTelegramChannel() }
        findViewById<LinearLayout>(R.id.btnOpenSettings).setOnClickListener {
            drawerOverlay.visibility = View.GONE
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadActiveProject()
    }

    override fun onResume() {
        super.onResume()
        loadActiveProject()
        webView.evaluateJavascript("if(window.MangoEditor) window.MangoEditor.setFontSize(${prefs.fontSize});", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerOverlay.visibility == View.VISIBLE) {
            drawerOverlay.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    private fun loadActiveProject() {
        val proj = if (prefs.activeProject.isNotEmpty()) {
            projectManager.getProjectDir(prefs.activeProject)
        } else {
            projectManager.ensureDefaultProject()
        }
        prefs.activeProject = proj.name
        currentProject = proj
        tvProjectTitle.text = proj.name
        val tree = projectManager.listTree(proj)
        adapter.submitList(tree)

        if (currentFile == null || !currentFile!!.exists() || !currentFile!!.path.startsWith(proj.path)) {
            val firstFile = tree.firstOrNull { !it.isDirectory }?.file
            if (firstFile != null) {
                webView.postDelayed({ openFile(firstFile) }, 350)
            }
        }
    }

    private fun openFile(file: File) {
        currentFile = file
        val content = runCatching { file.readText() }.getOrDefault("")
        latestContent = content
        val rel = currentProject?.let { file.relativeTo(it).path } ?: file.name
        tvActiveFile.text = rel
        tvStatus.text = "تم فتح $rel"
        val escaped = JSONObject.quote(content)
        val ext = JSONObject.quote(file.extension.ifEmpty { "txt" })
        webView.evaluateJavascript(
            "if(window.MangoEditor){ window.MangoEditor.setFontSize(${prefs.fontSize}); window.MangoEditor.setContent($escaped, $ext); }",
            null
        )
    }

    private fun saveCurrentFile(showToast: Boolean) {
        val file = currentFile ?: return
        runCatching {
            file.writeText(latestContent)
            tvStatus.text = "تم حفظ ${file.name}"
            if (showToast) Toast.makeText(this, "تم حفظ الملف", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "تعذر الحفظ: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun previewHtmlIfPossible() {
        val proj = currentProject ?: return
        val htmlFile = if (currentFile?.extension?.lowercase() == "html") currentFile else File(proj, "index.html")
        if (htmlFile == null || !htmlFile.exists()) {
            Toast.makeText(this, "المعاينة المباشرة متاحة لملفات HTML. استخدم البناء السحابي لتطبيقات أندرويد.", Toast.LENGTH_LONG).show()
            return
        }
        saveCurrentFile(false)
        val previewWeb = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            loadUrl("file://${htmlFile.absolutePath}")
        }
        AlertDialog.Builder(this)
            .setTitle("معاينة: ${htmlFile.name}")
            .setView(previewWeb)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun showProjectSwitcher() {
        val projects = projectManager.listProjects()
        val names = projects.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("اختر مشروعاً")
            .setItems(names) { _, which ->
                prefs.activeProject = projects[which].name
                currentFile = null
                loadActiveProject()
                drawerOverlay.visibility = View.GONE
            }
            .setPositiveButton("قالب جديد") { _, _ ->
                drawerOverlay.visibility = View.GONE
                startActivity(Intent(this, TemplatesActivity::class.java))
            }
            .show()
    }

    private fun showCreateFileDialog() {
        val proj = currentProject ?: return
        val input = EditText(this).apply {
            hint = "مثال: src/Helper.kt أو index.html"
        }
        AlertDialog.Builder(this)
            .setTitle("إنشاء ملف جديد")
            .setView(input)
            .setPositiveButton("إنشاء") { _, _ ->
                val rel = input.text.toString().trim()
                if (rel.isNotEmpty()) {
                    val f = File(proj, rel)
                    f.parentFile?.mkdirs()
                    if (!f.exists()) f.writeText("")
                    loadActiveProject()
                    openFile(f)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showNodeActions(node: FileNode) {
        AlertDialog.Builder(this)
            .setTitle(node.file.name)
            .setItems(arrayOf("حذف")) { _, _ ->
                node.file.deleteRecursively()
                if (currentFile == node.file) currentFile = null
                loadActiveProject()
            }
            .show()
    }

    private fun exportCurrentProjectZip() {
        val proj = currentProject ?: return
        saveCurrentFile(false)
        runCatching {
            val zip = projectManager.exportProjectZip(proj)
            Toast.makeText(this, "تم التصدير إلى: ${zip.absolutePath}", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, "تعذر التصدير: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTelegramChannel() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.telegram_url)))
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "قناة التليجرام: @apk3rade", Toast.LENGTH_SHORT).show()
        }
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun onContentChanged(content: String) {
            latestContent = content
        }

        @JavascriptInterface
        fun onAutoSave(content: String) {
            latestContent = content
            runOnUiThread { saveCurrentFile(false) }
        }
    }
}