package com.apk3rade.mangoide

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class FileNode(
    val file: File,
    val depth: Int,
    val isDirectory: Boolean
)

class ProjectManager(private val context: Context) {
    val rootDir: File
        get() = File(context.filesDir, "MangoProjects").apply { if (!exists()) mkdirs() }

    fun listProjects(): List<File> {
        return rootDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    fun ensureDefaultProject(): File {
        val existing = listProjects()
        if (existing.isNotEmpty()) return existing.first()
        val starter = ProjectTemplates.all().first()
        return createFromTemplate(starter, starter.defaultProjectName)
    }

    fun getProjectDir(name: String): File {
        val dir = File(rootDir, name)
        return if (dir.exists() && dir.isDirectory) dir else ensureDefaultProject()
    }

    fun createFromTemplate(template: TemplateSpec, requestedName: String): File {
        val cleanName = requestedName.trim().replace(Regex("[^a-zA-Z0-9_\-\u0600-\u06FF]"), "_")
            .ifEmpty { template.defaultProjectName }
        var target = File(rootDir, cleanName)
        var counter = 2
        while (target.exists()) {
            target = File(rootDir, "${cleanName}_$counter")
            counter++
        }
        target.mkdirs()
        for ((relPath, content) in template.files) {
            val f = File(target, relPath)
            f.parentFile?.mkdirs()
            f.writeText(content)
        }
        return target
    }

    fun listTree(projectDir: File): List<FileNode> {
        val result = mutableListOf<FileNode>()
        fun walk(dir: File, depth: Int) {
            val children = dir.listFiles()?.sortedWith(
                compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
            ) ?: return
            for (child in children) {
                if (child.name == ".git" || child.name == "build") continue
                result.add(FileNode(child, depth, child.isDirectory))
                if (child.isDirectory) walk(child, depth + 1)
            }
        }
        walk(projectDir, 0)
        return result
    }

    fun exportProjectZip(projectDir: File): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "${projectDir.name}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.relativeTo(projectDir).path.replace("\\", "/")
                zos.putNextEntry(ZipEntry(rel))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
