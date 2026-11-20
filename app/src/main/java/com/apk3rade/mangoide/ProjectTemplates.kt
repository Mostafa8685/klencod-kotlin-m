package com.apk3rade.mangoide

data class TemplateSpec(
    val id: String,
    val title: String,
    val description: String,
    val defaultProjectName: String,
    val files: Map<String, String>
)

object ProjectTemplates {
    fun all(): List<TemplateSpec> = listOf(
        TemplateSpec(
            id = "android_kotlin",
            title = "تطبيق أندرويد Kotlin جاهز",
            description = "مشروع Gradle 8.14 كامل مع ملف GitHub Actions للبناء السحابي بدون تطبيق خارجي.",
            defaultProjectName = "MyAndroidApp",
            files = mapOf(
                "settings.gradle" to "rootProject.name = \"MyAndroidApp\"\ninclude ':app'\n",
                "app/build.gradle" to "plugins {\n    id 'com.android.application'\n    id 'org.jetbrains.kotlin.android'\n}\nandroid {\n    namespace 'com.example.myapp'\n    compileSdk 34\n    defaultConfig {\n        applicationId 'com.example.myapp'\n        minSdk 24\n        targetSdk 34\n        versionCode 1\n        versionName '1.0'\n    }\n}\n",
                "app/src/main/java/com/example/myapp/MainActivity.kt" to "package com.example.myapp\n\nimport android.app.Activity\nimport android.os.Bundle\nimport android.widget.TextView\n\nclass MainActivity : Activity() {\n    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)\n        val tv = TextView(this)\n        tv.text = \"مرحباً من Mango IDE - Apk3rade\"\n        tv.textSize = 20f\n        setContentView(tv)\n    }\n}\n",
                ".github/workflows/build.yml" to "name: Cloud Build APK\non:\n  push:\n  workflow_dispatch:\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          java-version: '17'\n          distribution: 'temurin'\n      - uses: gradle/actions/setup-gradle@v4\n        with:\n          gradle-version: '8.14'\n      - run: gradle assembleDebug --no-daemon\n      - uses: actions/upload-artifact@v4\n        with:\n          name: app-debug-apk\n          path: app/build/outputs/apk/debug/*.apk\n"
            )
        ),
        TemplateSpec(
            id = "web_rtl",
            title = "تطبيق ويب عربي تفاعلي (HTML/CSS/JS)",
            description = "صفحة ويب عربية متجاوبة RTL يمكنك معاينتها مباشرة داخل Mango IDE.",
            defaultProjectName = "ArabicWebApp",
            files = mapOf(
                "index.html" to "<!DOCTYPE html>\n<html lang=\"ar\" dir=\"rtl\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>مشروعي على Mango IDE</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <main class=\"card\">\n    <h1>أهلاً بك في Mango IDE</h1>\n    <p>تم إنشاء هذا المشروع عبر Mango IDE التابع لقناة @apk3rade.</p>\n    <button id=\"btnCount\">عدد النقرات: 0</button>\n  </main>\n  <script src=\"app.js\"></script>\n</body>\n</html>\n",
                "style.css" to "body { margin: 0; font-family: sans-serif; background: #0B1120; color: #F8FAFC; display: grid; place-items: center; min-height: 100vh; }\n.card { background: #1E293B; padding: 24px; border-radius: 14px; border: 1px solid #334155; text-align: center; max-width: 340px; }\nbutton { margin-top: 14px; background: #F59E0B; color: #0B1120; border: 0; padding: 10px 18px; border-radius: 8px; font-weight: bold; }\n",
                "app.js" to "let count = 0;\nconst btn = document.getElementById('btnCount');\nbtn.addEventListener('click', () => {\n  count++;\n  btn.textContent = 'عدد النقرات: ' + count;\n});\n"
            )
        ),
        TemplateSpec(
            id = "flutter_starter",
            title = "مشروع Flutter / Dart أساسي",
            description = "هيكل مشروع فلاتر جاهز مع ملف pubspec.yaml وملف main.dart.",
            defaultProjectName = "MyFlutterApp",
            files = mapOf(
                "pubspec.yaml" to "name: my_flutter_app\ndescription: Built with Mango IDE\nversion: 1.0.0+1\nenvironment:\n  sdk: '>=3.0.0 <4.0.0'\ndependencies:\n  flutter:\n    sdk: flutter\n",
                "lib/main.dart" to "import 'package:flutter/material.dart';\n\nvoid main() => runApp(const MangoApp());\n\nclass MangoApp extends StatelessWidget {\n  const MangoApp({super.key});\n  @override\n  Widget build(BuildContext context) {\n    return MaterialApp(\n      debugShowCheckedModeBanner: false,\n      home: Scaffold(\n        appBar: AppBar(title: const Text('Mango IDE Flutter')),\n        body: const Center(child: Text('مرحباً من Mango IDE - @apk3rade')),\n      ),\n    );\n  }\n}\n"
            )
        ),
        TemplateSpec(
            id = "blank",
            title = "مشروع فارغ مخصص",
            description = "ابدأ بملف README نظيف ونظّم ملفاتك بالطريقة التي تناسبك.",
            defaultProjectName = "CustomProject",
            files = mapOf(
                "README.md" to "# مشروعي الجديد\n\nتم إنشاؤه باستخدام Mango IDE (@apk3rade).\n"
            )
        )
    )
}
