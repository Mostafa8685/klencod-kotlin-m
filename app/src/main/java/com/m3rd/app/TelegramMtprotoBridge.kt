package com.m3rd.app

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * جسر الاتصال الحقيقي ببروتوكول تيليجرام الرسمي (MTProto User Account & Saved Messages Bridge)
 *
 * يتصل مباشرة بخوادم تيليجرام الرسمية لتسجيل دخول حساب المستخدم الفعلي:
 * 1. إرسال كود التفعيل الحقيقي إلى تطبيق تيليجرام (auth.sendCode).
 * 2. تأكيد كود التفعيل (auth.signIn) ودعم كلمة مرور التحقق بخطوتين (2FA).
 * 3. توليد رابط رمز QR الفعلي (tg://login?token=...) للمسح من هاتف آخر.
 * 4. جلب ورفع وحذف الملفات مباشرة داخل محادثة "الرسائل المحفوظة" (Saved Messages / "me") في تيليجرام.
 */
object TelegramMtprotoBridge {

    interface Listener {
        fun onBridgeStatusChanged(status: String, message: String)
        fun onCodeSentSuccess(phoneCodeHash: String, viaApp: Boolean)
        fun onTwoFactorPasswordNeeded()
        fun onQrUrlReady(tgLoginUrl: String)
        fun onAuthenticatedSuccess(sessionString: String, displayName: String, phone: String)
        fun onSavedMessagesLoaded(entries: List<TelegramCloudEngine.CloudFileEntry>)
        fun onUploadProgressChanged(percent: Int)
        fun onUploadFinished(entry: TelegramCloudEngine.CloudFileEntry)
        fun onError(errorMessage: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var listener: Listener? = null
    private var pageLoaded = false
    private val pendingScripts = mutableListOf<String>()

    var isEngineReady: Boolean = false
        private set

    var lastStatusMessage: String = "جاري الاتصال بخوادم تيليجرام (MTProto)..."
        private set

    fun attachListener(l: Listener?) {
        listener = l
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun ensureInitialized(context: Context) {
        if (webView != null) return
        val appCtx = context.applicationContext
        val wv = WebView(appCtx)
        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        wv.addJavascriptInterface(JsBridge(appCtx), "AndroidBridge")
        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageLoaded = true
                flushPendingScripts()
            }
        }

        val html = try {
            appCtx.assets.open("mtproto_bridge.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Throwable) {
            "<html><body></body></html>"
        }

        // Load with https://web.telegram.org/ base URL so Telegram WebSocket DCs accept Origin
        wv.loadDataWithBaseURL(
            "https://web.telegram.org/",
            html,
            "text/html",
            "UTF-8",
            null
        )
        webView = wv
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            val wv = webView
            if (wv != null && pageLoaded) {
                wv.evaluateJavascript(script, null)
            } else {
                pendingScripts.add(script)
            }
        }
    }

    private fun flushPendingScripts() {
        val wv = webView ?: return
        val copy = ArrayList(pendingScripts)
        pendingScripts.clear()
        for (sc in copy) {
            wv.evaluateJavascript(sc, null)
        }
    }

    private fun jsEsc(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    fun restoreSavedSession(ctx: Context) {
        ensureInitialized(ctx)
        val s = TelegramCloudEngine.loadSession(ctx)
        if (s.sessionString.isNotBlank()) {
            evalJs("window.mtRestoreSession && window.mtRestoreSession('${jsEsc(s.sessionString)}', '${jsEsc(s.apiId)}', '${jsEsc(s.apiHash)}');")
        }
    }

    fun requestRealLoginCode(ctx: Context, phoneNumber: String, apiId: String, apiHash: String) {
        ensureInitialized(ctx)
        evalJs("window.mtSendLoginCode && window.mtSendLoginCode('${jsEsc(phoneNumber)}', '${jsEsc(apiId)}', '${jsEsc(apiHash)}');")
    }

    fun verifyRealPhoneCode(phoneCode: String, twoFaPassword: String = "") {
        evalJs("window.mtVerifyPhoneCode && window.mtVerifyPhoneCode('${jsEsc(phoneCode)}', '${jsEsc(twoFaPassword)}');")
    }

    fun submitRealTwoFactorPassword(twoFaPassword: String) {
        evalJs("window.mtSubmitTwoFactorPassword && window.mtSubmitTwoFactorPassword('${jsEsc(twoFaPassword)}');")
    }

    fun startRealQrLogin(ctx: Context, apiId: String, apiHash: String) {
        ensureInitialized(ctx)
        evalJs("window.mtStartQrLogin && window.mtStartQrLogin('${jsEsc(apiId)}', '${jsEsc(apiHash)}');")
    }

    fun syncSavedMessages() {
        evalJs("window.mtFetchSavedMessages && window.mtFetchSavedMessages();")
    }

    fun deleteMessageFromSavedMessages(telegramMessageId: Long) {
        evalJs("window.mtDeleteSavedMessage && window.mtDeleteSavedMessage($telegramMessageId);")
    }

    /**
     * يقرأ الملف من الهاتف ويرسله على شكل أجزاء إلى جسر MTProto ليتم رفعه فعلياً
     * إلى محادثة "الرسائل المحفوظة" (Saved Messages / "me") في حساب المستخدم على تيليجرام.
     */
    fun uploadUriToRealSavedMessages(
        ctx: Context,
        uri: Uri,
        folderPath: String,
        localUriStr: String
    ) {
        val resolver = ctx.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        var fileName = "ملف_${System.currentTimeMillis() % 10000}"

        try {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val n = c.getString(idx)
                        if (!n.isNullOrBlank()) fileName = n
                    }
                }
            }
        } catch (_: Throwable) {}

        val bytes = try {
            val bos = ByteArrayOutputStream()
            val ins: InputStream? = resolver.openInputStream(uri)
            ins?.use { input ->
                val buf = ByteArray(32 * 1024)
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    bos.write(buf, 0, r)
                }
            }
            bos.toByteArray()
        } catch (t: Throwable) {
            mainHandler.post {
                listener?.onError("تعذر قراءة الملف من الهاتف: ${t.localizedMessage ?: ""}")
            }
            return
        }

        if (bytes.isEmpty()) {
            mainHandler.post {
                listener?.onError("الملف المختار فارغ أو لا يمكن الوصول إليه")
            }
            return
        }

        lastUploadedLocalUri = localUriStr
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        evalJs("window.mtBeginFileUpload('${jsEsc(fileName)}', '${jsEsc(mimeType)}', '${jsEsc(folderPath)}');")

        val chunkSize = 48 * 1024
        var offset = 0
        while (offset < b64.length) {
            val end = (offset + chunkSize).coerceAtMost(b64.length)
            val part = b64.substring(offset, end)
            evalJs("window.mtAppendFileChunk('$part');")
            offset = end
        }

        evalJs("window.mtCommitFileUpload();")
    }

    @Volatile
    private var lastUploadedLocalUri: String = ""

    private class JsBridge(private val appCtx: Context) {

        @JavascriptInterface
        fun onBridgeStatus(status: String, detail: String) {
            isEngineReady = (status == "READY" || status == "DELETED")
            if (detail.isNotBlank()) {
                lastStatusMessage = detail
            }
            mainHandler.post {
                listener?.onBridgeStatusChanged(status, detail)
            }
        }

        @JavascriptInterface
        fun onCodeSentToTelegram(phoneCodeHash: String, viaApp: Boolean) {
            mainHandler.post {
                listener?.onCodeSentSuccess(phoneCodeHash, viaApp)
            }
        }

        @JavascriptInterface
        fun onTwoFactorPasswordNeeded() {
            mainHandler.post {
                listener?.onTwoFactorPasswordNeeded()
            }
        }

        @JavascriptInterface
        fun onQrLoginUrlGenerated(url: String) {
            mainHandler.post {
                listener?.onQrUrlReady(url)
            }
        }

        @JavascriptInterface
        fun onLoginSuccess(sessionString: String, displayName: String, phone: String) {
            val current = TelegramCloudEngine.loadSession(appCtx)
            TelegramCloudEngine.saveSession(
                appCtx,
                current.copy(
                    isLoggedIn = true,
                    sessionString = sessionString,
                    userName = displayName.ifBlank { "مستخدم تيليجرام" },
                    phoneNumber = phone.ifBlank { current.phoneNumber }
                )
            )
            mainHandler.post {
                listener?.onAuthenticatedSuccess(sessionString, displayName, phone)
            }
        }

        @JavascriptInterface
        fun onSavedMessagesSynced(jsonArrayStr: String) {
            val syncedList = mutableListOf<TelegramCloudEngine.CloudFileEntry>()
            try {
                val arr = JSONArray(jsonArrayStr)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    syncedList.add(
                        TelegramCloudEngine.CloudFileEntry(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "file"),
                            sizeBytes = o.optLong("sizeBytes", 0L),
                            mimeType = o.optString("mimeType", "application/octet-stream"),
                            folderPath = o.optString("folderPath", ""),
                            isImageOrVideo = o.optBoolean("isImageOrVideo", false),
                            isVideo = o.optBoolean("isVideo", false),
                            telegramMessageId = o.optLong("telegramMessageId", 0L),
                            telegramFileId = "tg_msg_${o.optLong("telegramMessageId", 0L)}",
                            uploadedAt = o.optLong("uploadedAt", System.currentTimeMillis()),
                            localUriString = ""
                        )
                    )
                }
            } catch (_: Throwable) {}

            if (syncedList.isNotEmpty()) {
                val existing = TelegramCloudEngine.loadCloudFiles(appCtx)
                val merged = mutableListOf<TelegramCloudEngine.CloudFileEntry>()
                merged.addAll(syncedList)
                for (old in existing) {
                    if (merged.none { it.telegramMessageId == old.telegramMessageId && old.telegramMessageId > 0L }) {
                        merged.add(old)
                    } else {
                        // Preserve localUriString if we uploaded it from this device
                        val idx = merged.indexOfFirst { it.telegramMessageId == old.telegramMessageId }
                        if (idx >= 0 && old.localUriString.isNotBlank()) {
                            merged[idx] = merged[idx].copy(localUriString = old.localUriString)
                        }
                    }
                }
                TelegramCloudEngine.saveCloudFiles(appCtx, merged)
            }

            mainHandler.post {
                listener?.onSavedMessagesLoaded(TelegramCloudEngine.loadCloudFiles(appCtx))
            }
        }

        @JavascriptInterface
        fun onUploadProgress(percent: Int) {
            mainHandler.post {
                listener?.onUploadProgressChanged(percent)
            }
        }

        @JavascriptInterface
        fun onFileUploadedToSavedMessages(
            fileName: String,
            sizeBytes: Long,
            mimeType: String,
            folderPath: String,
            msgId: Long
        ) {
            val lowMime = mimeType.lowercase()
            val lowName = fileName.lowercase()
            val isVid = lowMime.startsWith("video/") || lowName.endsWith(".mp4") || lowName.endsWith(".mkv") || lowName.endsWith(".mov")
            val isImg = lowMime.startsWith("image/") || lowName.endsWith(".jpg") || lowName.endsWith(".jpeg") || lowName.endsWith(".png") || lowName.endsWith(".webp")

            val entry = TelegramCloudEngine.CloudFileEntry(
                id = "tg_$msgId",
                name = fileName,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                folderPath = folderPath,
                isImageOrVideo = isImg || isVid,
                isVideo = isVid,
                telegramMessageId = msgId,
                telegramFileId = "tg_saved_$msgId",
                uploadedAt = System.currentTimeMillis(),
                localUriString = lastUploadedLocalUri
            )

            val files = TelegramCloudEngine.loadCloudFiles(appCtx)
            files.removeAll { it.telegramMessageId == msgId }
            files.add(0, entry)
            TelegramCloudEngine.saveCloudFiles(appCtx, files)

            TelegramCloudEngine.recordTransferPublic(
                appCtx,
                TelegramCloudEngine.TransferRecord(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    sizeBytes = sizeBytes,
                    direction = "UPLOAD",
                    status = "مرفوع فعلياً في الرسائل المحفوظة (#$msgId)",
                    timestamp = System.currentTimeMillis(),
                    telegramMsgId = msgId
                )
            )

            mainHandler.post {
                listener?.onUploadFinished(entry)
            }
        }

        @JavascriptInterface
        fun onMtprotoError(errorMsg: String) {
            val translated = translateTelegramError(errorMsg)
            mainHandler.post {
                listener?.onError(translated)
            }
        }

        private fun translateTelegramError(raw: String): String {
            return when {
                raw.contains("PHONE_NUMBER_INVALID", ignoreCase = true) ->
                    "رقم الهاتف غير صحيح — تأكد من كتابة مفتاح الدولة مثل +2010..."
                raw.contains("PHONE_CODE_INVALID", ignoreCase = true) ->
                    "كود التفعيل الذي أدخلته غير صحيح — تأكد من آخر رسالة وصلتك في تيليجرام"
                raw.contains("PHONE_CODE_EXPIRED", ignoreCase = true) ->
                    "انتهت صلاحية كود التفعيل — اضغط على إعادة إرسال الكود"
                raw.contains("PASSWORD_HASH_INVALID", ignoreCase = true) ->
                    "كلمة مرور التحقق بخطوتين (2FA) غير صحيحة"
                raw.contains("FLOOD_WAIT", ignoreCase = true) ->
                    "محاولات كثيرة متتالية — يرجى الانتظار قليلاً ثم المحاولة"
                else -> "رسالة خادم تيليجرام: $raw"
            }
        }
    }
}
