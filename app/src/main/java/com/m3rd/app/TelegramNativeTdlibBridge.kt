package com.m3rd.app

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.Executors

/**
 * محرك TDLib الأصلي للأندرويد (Native C++ TDLib Client Bridge)
 *
 * يستخدم مكتبة تيليجرام الرسمية المترجمة للأندرويد (libtdjni.so / com.github.tdlibx:td:1.8.56)
 * للاتصال الفعلي المباشر بخوادم تيليجرام (MTProto) بدون أي WebView أو وسطاء:
 * 1. إرسال رقم الهاتف عبر SetAuthenticationPhoneNumber واستلام الكود الحقيقي على تطبيق تيليجرام.
 * 2. تأكيد الكود عبر CheckAuthenticationCode ودعم كلمة المرور السحابية (CheckAuthenticationPassword).
 * 3. دعم تسجيل الدخول عبر رمز QR الرسمي (RequestQrCodeAuthentication).
 * 4. فتح محادثة "الرسائل المحفوظة" (Saved Messages) تلقائياً عبر CreatePrivateChat(myUserId)
 *    ورفع الصور والفيديوهات والملفات إليها فعلياً عبر SendMessage + InputMessageDocument.
 */
object TelegramNativeTdlibBridge {

    interface Listener {
        fun onBridgeStatusChanged(status: String, message: String)
        fun onCodeSentSuccess(codeDeliveryInfo: String)
        fun onTwoFactorPasswordNeeded(passwordHint: String)
        fun onQrUrlReady(tgLoginUrl: String)
        fun onAuthenticatedSuccess(displayName: String, phone: String, userId: Long)
        fun onSavedMessagesLoaded(entries: List<TelegramCloudEngine.CloudFileEntry>)
        fun onUploadProgressChanged(percent: Int)
        fun onUploadFinished(entry: TelegramCloudEngine.CloudFileEntry)
        fun onError(errorMessage: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    @Volatile private var appContext: Context? = null
    @Volatile private var listener: Listener? = null
    @Volatile private var tdClientInstance: Any? = null
    @Volatile private var clientSendMethod: Method? = null
    @Volatile private var resultHandlerInterface: Class<*>? = null
    @Volatile private var activePackagePrefix: String = "org.drinkless.tdlib"

    @Volatile var currentAuthStateName: String = "NONE"
        private set

    @Volatile var isTdlibReadyForAuth: Boolean = false
        private set

    @Volatile var isAuthorized: Boolean = false
        private set

    @Volatile var myUserId: Long = 0L
        private set

    @Volatile var savedMessagesChatId: Long = 0L
        private set

    @Volatile private var pendingPhoneToSubmit: String = ""
    @Volatile private var pendingQrRequest: Boolean = false

    fun attachListener(l: Listener?) {
        listener = l
    }

    fun ensureInitialized(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (tdClientInstance != null) return

        worker.execute {
            try {
                // Ensure native library is loaded
                try {
                    System.loadLibrary("tdjni")
                } catch (_: Throwable) {}

                val pkgCandidates = listOf(
                    "org.drinkless.tdlib",
                    "org.drinkless.td.libcore.telegram"
                )
                var foundClientClass: Class<*>? = null
                for (pkg in pkgCandidates) {
                    try {
                        foundClientClass = Class.forName("$pkg.Client")
                        activePackagePrefix = pkg
                        break
                    } catch (_: Throwable) {}
                }

                val clientClass = foundClientClass
                    ?: throw IllegalStateException("لم يتم العثور على كلاس TDLib Client في الحزمة")

                val resHandlerCls = Class.forName("$activePackagePrefix.Client\$ResultHandler")
                val excHandlerCls = Class.forName("$activePackagePrefix.Client\$ExceptionHandler")
                resultHandlerInterface = resHandlerCls

                // Quiet TDLib internal verbose logs
                try {
                    val setLogVerbosity = newTdObject("SetLogVerbosityLevel").apply {
                        setField(this, "newVerbosityLevel", 1)
                    }
                    val execMethod = clientClass.methods.firstOrNull { it.name == "execute" && it.parameterTypes.size == 1 }
                    execMethod?.invoke(null, setLogVerbosity)
                } catch (_: Throwable) {}

                val updateProxy = createResultHandlerProxy { obj ->
                    handleIncomingTdObject(obj)
                }
                val excProxy = Proxy.newProxyInstance(
                    excHandlerCls.classLoader,
                    arrayOf(excHandlerCls)
                ) { _, _, _ -> null }

                val createMethod = clientClass.getMethod(
                    "create",
                    resHandlerCls,
                    excHandlerCls,
                    excHandlerCls
                )
                val clientObj = createMethod.invoke(null, updateProxy, excProxy, excProxy)
                tdClientInstance = clientObj
                clientSendMethod = clientClass.methods.firstOrNull {
                    it.name == "send" && it.parameterTypes.size == 2
                }

                notifyStatus("INITIALIZED", "تم تشغيل محرك تيليجرام الأصلي (TDLib C++)")
            } catch (t: Throwable) {
                notifyError("تعذر تهيئة محرك TDLib الأصلي: ${t.localizedMessage ?: t.javaClass.simpleName}")
            }
        }
    }

    private fun createResultHandlerProxy(onResultCallback: (Any) -> Unit): Any {
        val iface = resultHandlerInterface ?: return Any()
        return Proxy.newProxyInstance(
            iface.classLoader,
            arrayOf(iface),
            InvocationHandler { _, method, args ->
                if (method.name == "onResult" && args != null && args.isNotEmpty() && args[0] != null) {
                    try {
                        onResultCallback(args[0])
                    } catch (t: Throwable) {
                        notifyError("خطأ في معالجة رد تيليجرام: ${t.localizedMessage ?: ""}")
                    }
                }
                null
            }
        )
    }

    fun sendTdRequest(requestObj: Any, onResponse: ((Any) -> Unit)? = null) {
        worker.execute {
            try {
                val client = tdClientInstance
                val sendMeth = clientSendMethod
                if (client == null || sendMeth == null) {
                    notifyError("محرك TDLib قيد التشغيل، يرجى المحاولة بعد لحظة")
                    return@execute
                }
                val handlerProxy = createResultHandlerProxy { resp ->
                    if (resp.javaClass.simpleName == "Error") {
                        val code = getField(resp, "code") as? Int ?: -1
                        val msg = getField(resp, "message") as? String ?: "Unknown error"
                        notifyError(translateTdlibError(code, msg))
                    } else {
                        onResponse?.invoke(resp)
                    }
                }
                sendMeth.invoke(client, requestObj, handlerProxy)
            } catch (t: Throwable) {
                notifyError("فشل إرسال الطلب إلى TDLib: ${t.localizedMessage ?: ""}")
            }
        }
    }

    private fun handleIncomingTdObject(obj: Any) {
        when (obj.javaClass.simpleName) {
            "UpdateAuthorizationState" -> {
                val state = getField(obj, "authorizationState")
                if (state != null) {
                    handleAuthorizationState(state)
                }
            }
            "UpdateFile" -> {
                val fileObj = getField(obj, "file") ?: return
                val expectedSize = (getField(fileObj, "expectedSize") as? Long)
                    ?: (getField(fileObj, "size") as? Long)
                    ?: (getField(fileObj, " size") as? Int)?.toLong()
                    ?: 0L
                val remoteObj = getField(fileObj, "remote")
                if (remoteObj != null && expectedSize > 0L) {
                    val isUploading = getField(remoteObj, "isUploadingActive") as? Boolean ?: false
                    val uploaded = (getField(remoteObj, "uploadedSize") as? Long)
                        ?: (getField(remoteObj, "uploadedSize") as? Int)?.toLong()
                        ?: 0L
                    if (isUploading) {
                        val pct = ((uploaded * 100L) / expectedSize).toInt().coerceIn(1, 99)
                        mainHandler.post { listener?.onUploadProgressChanged(pct) }
                    }
                }
            }
        }
    }

    private fun handleAuthorizationState(state: Any) {
        val stateName = state.javaClass.simpleName
        currentAuthStateName = stateName

        when (stateName) {
            "AuthorizationStateWaitTdlibParameters" -> {
                val ctx = appContext ?: return
                val session = TelegramCloudEngine.loadSession(ctx)
                val dbDir = File(ctx.filesDir, "tdlib_db").apply { mkdirs() }.absolutePath
                val filesDir = File(ctx.filesDir, "tdlib_files").apply { mkdirs() }.absolutePath
                val parsedApiId = session.apiId.trim().toIntOrNull() ?: 94575
                val parsedApiHash = session.apiHash.trim().ifBlank { "a3406de8d171bb422bb6ddf3bbd800e2" }

                val req = newTdObject("SetTdlibParameters")
                val paramsField = findField(req.javaClass, "parameters")
                if (paramsField != null) {
                    // Older TDLib structure where SetTdlibParameters wraps TdlibParameters
                    val tdParams = newTdObject("TdlibParameters")
                    populateTdlibParametersObject(tdParams, dbDir, filesDir, parsedApiId, parsedApiHash)
                    setField(req, "parameters", tdParams)
                } else {
                    // Newer TDLib 1.8.x structure where fields are directly on SetTdlibParameters
                    populateTdlibParametersObject(req, dbDir, filesDir, parsedApiId, parsedApiHash)
                }
                sendTdRequest(req)
            }

            "AuthorizationStateWaitEncryptionKey" -> {
                val req = newTdObject("CheckDatabaseEncryptionKey").apply {
                    setField(this, "encryptionKey", ByteArray(0))
                }
                sendTdRequest(req)
            }

            "AuthorizationStateWaitPhoneNumber" -> {
                isTdlibReadyForAuth = true
                notifyStatus("READY", "متصل بخوادم تيليجرام الرسمية (جاهز لتسجيل الدخول)")
                if (pendingPhoneToSubmit.isNotBlank()) {
                    val phone = pendingPhoneToSubmit
                    pendingPhoneToSubmit = ""
                    submitPhoneNumberInternal(phone)
                } else if (pendingQrRequest) {
                    pendingQrRequest = false
                    requestQrLoginInternal()
                }
            }

            "AuthorizationStateWaitOtherDeviceConfirmation" -> {
                val link = getField(state, "link") as? String ?: ""
                if (link.isNotBlank()) {
                    mainHandler.post {
                        listener?.onQrUrlReady(link)
                    }
                }
            }

            "AuthorizationStateWaitCode" -> {
                val codeInfo = getField(state, "codeInfo")
                val phone = (if (codeInfo != null) getField(codeInfo, "phoneNumber") as? String else null).orEmpty()
                mainHandler.post {
                    listener?.onCodeSentSuccess(phone)
                }
            }

            "AuthorizationStateWaitPassword" -> {
                val hint = getField(state, "passwordHint") as? String ?: ""
                mainHandler.post {
                    listener?.onTwoFactorPasswordNeeded(hint)
                }
            }

            "AuthorizationStateReady" -> {
                isAuthorized = true
                fetchMyProfileAndOpenSavedMessages()
            }

            "AuthorizationStateClosed" -> {
                isAuthorized = false
                isTdlibReadyForAuth = false
                tdClientInstance = null
                appContext?.let { ensureInitialized(it) }
            }
        }
    }

    private fun populateTdlibParametersObject(
        target: Any,
        dbDir: String,
        filesDir: String,
        apiId: Int,
        apiHash: String
    ) {
        setField(target, "useTestDc", false)
        setField(target, "databaseDirectory", dbDir)
        setField(target, "filesDirectory", filesDir)
        setField(target, "databaseEncryptionKey", ByteArray(0))
        setField(target, "useFileDatabase", true)
        setField(target, "useChatInfoDatabase", true)
        setField(target, "useMessageDatabase", true)
        setField(target, "useSecretChats", false)
        setField(target, "apiId", apiId)
        setField(target, "apiHash", apiHash)
        setField(target, "systemLanguageCode", "ar")
        setField(target, "deviceModel", Build.MODEL ?: "Android Phone")
        setField(target, "systemVersion", Build.VERSION.RELEASE ?: "Android")
        setField(target, "applicationVersion", "7.0-Unlim")
        setField(target, "enableStorageOptimizer", true)
        setField(target, "ignoreFileNames", false)
    }

    /**
     * يرسل رقم الهاتف الفعلي إلى تيليجرام لإرسال كود التفعيل المكون من 5 أرقام
     */
    fun requestRealLoginCode(ctx: Context, phoneNumber: String, apiId: String, apiHash: String) {
        ensureInitialized(ctx)
        val cleanPhone = phoneNumber.trim()
        val s = TelegramCloudEngine.loadSession(ctx)
        TelegramCloudEngine.saveSession(
            ctx,
            s.copy(
                phoneNumber = cleanPhone,
                apiId = apiId.trim().ifBlank { "94575" },
                apiHash = apiHash.trim().ifBlank { "a3406de8d171bb422bb6ddf3bbd800e2" }
            )
        )

        if (currentAuthStateName == "AuthorizationStateWaitCode") {
            // إذا كان الكود مرسلاً بالفعل لنفس الرقم نفتح شاشة إدخال الكود مباشرة
            mainHandler.post {
                listener?.onCodeSentSuccess(cleanPhone)
            }
            return
        }

        if (!isTdlibReadyForAuth && currentAuthStateName != "AuthorizationStateWaitOtherDeviceConfirmation") {
            pendingPhoneToSubmit = cleanPhone
            notifyStatus("CONNECTING", "جاري تهيئة الاتصال بخادم تيليجرام وإرسال الكود...")
            return
        }

        submitPhoneNumberInternal(cleanPhone)
    }

    private fun submitPhoneNumberInternal(phoneNumber: String) {
        val req = newTdObject("SetAuthenticationPhoneNumber").apply {
            setField(this, "phoneNumber", phoneNumber)
            setField(this, "settings", null)
        }
        sendTdRequest(req)
    }

    /**
     * يتحقق من كود التفعيل المكون من 5 أرقام الذي وصل للمستخدم في تيليجرام
     */
    fun verifyRealPhoneCode(code: String) {
        val req = newTdObject("CheckAuthenticationCode").apply {
            setField(this, "code", code.trim())
        }
        sendTdRequest(req)
    }

    /**
     * يتحقق من كلمة مرور التحقق بخطوتين (2FA) إن كانت مفعلة على الحساب
     */
    fun submitRealTwoFactorPassword(password: String) {
        val req = newTdObject("CheckAuthenticationPassword").apply {
            setField(this, "password", password)
        }
        sendTdRequest(req)
    }

    /**
     * يطلب توليد رابط رمز QR الفعلي (tg://login?token=...) من خادم تيليجرام
     */
    fun startRealQrLogin(ctx: Context) {
        ensureInitialized(ctx)
        if (!isTdlibReadyForAuth) {
            pendingQrRequest = true
            return
        }
        requestQrLoginInternal()
    }

    private fun requestQrLoginInternal() {
        val req = newTdObject("RequestQrCodeAuthentication").apply {
            setField(this, "otherUserIds", LongArray(0))
        }
        sendTdRequest(req)
    }

    /**
     * يجلب بيانات المستخدم الحالي ثم يفتح محادثة "الرسائل المحفوظة" (Saved Messages = Private Chat with Self)
     */
    private fun fetchMyProfileAndOpenSavedMessages() {
        val getMeReq = newTdObject("GetMe")
        sendTdRequest(getMeReq) { userObj ->
            val uId = (getField(userObj, "id") as? Long)
                ?: (getField(userObj, "id") as? Int)?.toLong()
                ?: 0L
            val firstName = getField(userObj, "firstName") as? String ?: ""
            val lastName = getField(userObj, "lastName") as? String ?: ""
            val phone = getField(userObj, "phoneNumber") as? String ?: ""
            val fullName = "$firstName $lastName".trim().ifBlank { "مستخدم تيليجرام" }

            myUserId = uId
            val ctx = appContext
            if (ctx != null) {
                val cur = TelegramCloudEngine.loadSession(ctx)
                TelegramCloudEngine.saveSession(
                    ctx,
                    cur.copy(
                        isLoggedIn = true,
                        sessionString = "tdlib_native_user_$uId",
                        userName = fullName,
                        phoneNumber = if (phone.isNotBlank()) "+$phone" else cur.phoneNumber
                    )
                )
            }

            mainHandler.post {
                listener?.onAuthenticatedSuccess(fullName, phone, uId)
            }

            if (uId != 0L) {
                val createChatReq = newTdObject("CreatePrivateChat").apply {
                    setField(this, "userId", uId)
                    setField(this, "force", false)
                }
                sendTdRequest(createChatReq) { chatObj ->
                    val cId = (getField(chatObj, "id") as? Long) ?: uId
                    savedMessagesChatId = cId
                    syncSavedMessages()
                }
            }
        }
    }

    /**
     * يجلب الملفات والصور والفيديوهات المحفوظة فعلياً في قسم "الرسائل المحفوظة" (Saved Messages)
     */
    fun syncSavedMessages() {
        val chatId = if (savedMessagesChatId != 0L) savedMessagesChatId else myUserId
        if (chatId == 0L) {
            if (isAuthorized) fetchMyProfileAndOpenSavedMessages()
            return
        }

        val historyReq = newTdObject("GetChatHistory").apply {
            setField(this, "chatId", chatId)
            setField(this, "fromMessageId", 0L)
            setField(this, "offset", 0)
            setField(this, "limit", 60)
            setField(this, "onlyLocal", false)
        }

        sendTdRequest(historyReq) { messagesObj ->
            val msgArray = getField(messagesObj, "messages") as? Array<*> ?: emptyArray<Any>()
            val parsedEntries = mutableListOf<TelegramCloudEngine.CloudFileEntry>()

            for (m in msgArray) {
                if (m == null) continue
                val msgId = (getField(m, "id") as? Long) ?: 0L
                val dateSec = (getField(m, "date") as? Int)?.toLong() ?: (System.currentTimeMillis() / 1000L)
                val content = getField(m, "content") ?: continue
                val contentType = content.javaClass.simpleName

                val captionObj = getField(content, "caption")
                val captionText = (if (captionObj != null) getField(captionObj, "text") as? String else null).orEmpty()
                var folderPath = ""
                val mPath = Regex("Path:\\s*/Files/([^\\s|]+)").find(captionText)
                if (mPath != null) {
                    folderPath = mPath.groupValues[1].trim()
                }

                when (contentType) {
                    "MessageDocument" -> {
                        val doc = getField(content, "document") ?: continue
                        val fileName = (getField(doc, "fileName") as? String).orEmpty().ifBlank { "ملف_$msgId" }
                        val mimeType = (getField(doc, "mimeType") as? String).orEmpty().ifBlank { "application/octet-stream" }
                        val docFile = getField(doc, "document")
                        val sizeBytes = extractTdFileSize(docFile)
                        val lowMime = mimeType.lowercase()
                        val lowName = fileName.lowercase()
                        val isVid = lowMime.startsWith("video/") || lowName.endsWith(".mp4") || lowName.endsWith(".mkv")
                        val isImg = lowMime.startsWith("image/") || lowName.endsWith(".jpg") || lowName.endsWith(".jpeg") || lowName.endsWith(".png") || lowName.endsWith(".webp")

                        parsedEntries.add(
                            TelegramCloudEngine.CloudFileEntry(
                                id = "td_$msgId",
                                name = fileName,
                                sizeBytes = sizeBytes,
                                mimeType = mimeType,
                                folderPath = folderPath,
                                isImageOrVideo = isImg || isVid,
                                isVideo = isVid,
                                telegramMessageId = msgId,
                                telegramFileId = "td_doc_$msgId",
                                uploadedAt = dateSec * 1000L
                            )
                        )
                    }
                    "MessagePhoto" -> {
                        parsedEntries.add(
                            TelegramCloudEngine.CloudFileEntry(
                                id = "td_$msgId",
                                name = "صورة_محفوظة_$msgId.jpg",
                                sizeBytes = 512_000L,
                                mimeType = "image/jpeg",
                                folderPath = folderPath,
                                isImageOrVideo = true,
                                isVideo = false,
                                telegramMessageId = msgId,
                                telegramFileId = "td_photo_$msgId",
                                uploadedAt = dateSec * 1000L
                            )
                        )
                    }
                    "MessageVideo" -> {
                        val vid = getField(content, "video")
                        val fileName = (if (vid != null) getField(vid, "fileName") as? String else null).orEmpty().ifBlank { "فيديو_$msgId.mp4" }
                        val vidFile = if (vid != null) getField(vid, "video") else null
                        val sz = extractTdFileSize(vidFile)
                        parsedEntries.add(
                            TelegramCloudEngine.CloudFileEntry(
                                id = "td_$msgId",
                                name = fileName,
                                sizeBytes = sz,
                                mimeType = "video/mp4",
                                folderPath = folderPath,
                                isImageOrVideo = true,
                                isVideo = true,
                                telegramMessageId = msgId,
                                telegramFileId = "td_vid_$msgId",
                                uploadedAt = dateSec * 1000L
                            )
                        )
                    }
                    "MessageAudio" -> {
                        val aud = getField(content, "audio")
                        val fileName = (if (aud != null) getField(aud, "fileName") as? String else null).orEmpty().ifBlank { "صوت_$msgId.mp3" }
                        val audFile = if (aud != null) getField(aud, "audio") else null
                        val sz = extractTdFileSize(audFile)
                        parsedEntries.add(
                            TelegramCloudEngine.CloudFileEntry(
                                id = "td_$msgId",
                                name = fileName,
                                sizeBytes = sz,
                                mimeType = "audio/mpeg",
                                folderPath = folderPath,
                                isImageOrVideo = false,
                                isVideo = false,
                                telegramMessageId = msgId,
                                telegramFileId = "td_aud_$msgId",
                                uploadedAt = dateSec * 1000L
                            )
                        )
                    }
                }
            }

            val ctx = appContext
            if (ctx != null && parsedEntries.isNotEmpty()) {
                val existing = TelegramCloudEngine.loadCloudFiles(ctx)
                val merged = mutableListOf<TelegramCloudEngine.CloudFileEntry>()
                merged.addAll(parsedEntries)
                for (old in existing) {
                    if (merged.none { it.telegramMessageId == old.telegramMessageId && old.telegramMessageId > 0L }) {
                        merged.add(old)
                    }
                }
                TelegramCloudEngine.saveCloudFiles(ctx, merged)
            }

            mainHandler.post {
                val ctx2 = appContext
                if (ctx2 != null) {
                    listener?.onSavedMessagesLoaded(TelegramCloudEngine.loadCloudFiles(ctx2))
                }
            }
        }
    }

    private fun extractTdFileSize(tdFileObj: Any?): Long {
        if (tdFileObj == null) return 0L
        return (getField(tdFileObj, "size") as? Long)
            ?: (getField(tdFileObj, "size") as? Int)?.toLong()
            ?: (getField(tdFileObj, "expectedSize") as? Long)
            ?: (getField(tdFileObj, "expectedSize") as? Int)?.toLong()
            ?: 0L
    }

    /**
     * يرفع أي ملف أو صورة أو فيديو من الهاتف فعلياً إلى قسم "الرسائل المحفوظة" (Saved Messages) في تيليجرام
     * باستخدام InputMessageDocument للحفاظ على الجودة الأصلية بدون ضغط تماماً مثل Unlim Cloud.
     */
    fun uploadUriToRealSavedMessages(
        ctx: Context,
        uri: Uri,
        folderPath: String,
        localUriStr: String
    ) {
        worker.execute {
            try {
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

                val stagingDir = File(ctx.cacheDir, "tdlib_upload_staging").apply { mkdirs() }
                val safeFileName = fileName.replace("/", "_").replace("\\", "_")
                val stagedFile = File(stagingDir, safeFileName)

                val ins: InputStream? = resolver.openInputStream(uri)
                if (ins == null) {
                    notifyError("تعذر قراءة الملف المختار من الهاتف")
                    return@execute
                }
                ins.use { input ->
                    FileOutputStream(stagedFile).use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            output.write(buf, 0, r)
                        }
                    }
                }

                val targetChat = if (savedMessagesChatId != 0L) savedMessagesChatId else myUserId
                if (targetChat == 0L) {
                    notifyError("جاري فتح محادثة الرسائل المحفوظة... يرجى المحاولة بعد ثانية")
                    fetchMyProfileAndOpenSavedMessages()
                    return@execute
                }

                val pathTag = if (folderPath.isBlank()) "/Files" else "/Files/$folderPath"
                val captionStr = "UnlimCloud | Path: $pathTag | File: $fileName"

                val inputFileLocal = newTdObject("InputFileLocal").apply {
                    setField(this, "path", stagedFile.absolutePath)
                }
                val formattedCaption = newTdObject("FormattedText").apply {
                    setField(this, "text", captionStr)
                }
                val inputMsgDoc = newTdObject("InputMessageDocument").apply {
                    setField(this, "document", inputFileLocal)
                    setField(this, "caption", formattedCaption)
                    setField(this, "disableContentTypeDetection", true)
                }
                val sendMsgReq = newTdObject("SendMessage").apply {
                    setField(this, "chatId", targetChat)
                    setField(this, "inputMessageContent", inputMsgDoc)
                }

                sendTdRequest(sendMsgReq) { sentMsgObj ->
                    val msgId = (getField(sentMsgObj, "id") as? Long) ?: (System.currentTimeMillis() / 100L)
                    val lowMime = mimeType.lowercase()
                    val lowName = fileName.lowercase()
                    val isVid = lowMime.startsWith("video/") || lowName.endsWith(".mp4") || lowName.endsWith(".mkv")
                    val isImg = lowMime.startsWith("image/") || lowName.endsWith(".jpg") || lowName.endsWith(".jpeg") || lowName.endsWith(".png") || lowName.endsWith(".webp")

                    val entry = TelegramCloudEngine.CloudFileEntry(
                        id = "td_$msgId",
                        name = fileName,
                        sizeBytes = stagedFile.length(),
                        mimeType = mimeType,
                        folderPath = folderPath,
                        isImageOrVideo = isImg || isVid,
                        isVideo = isVid,
                        telegramMessageId = msgId,
                        telegramFileId = "td_saved_$msgId",
                        uploadedAt = System.currentTimeMillis(),
                        localUriString = localUriStr
                    )

                    val files = TelegramCloudEngine.loadCloudFiles(ctx)
                    files.removeAll { it.telegramMessageId == msgId }
                    files.add(0, entry)
                    TelegramCloudEngine.saveCloudFiles(ctx, files)

                    TelegramCloudEngine.recordTransferPublic(
                        ctx,
                        TelegramCloudEngine.TransferRecord(
                            id = UUID.randomUUID().toString(),
                            fileName = fileName,
                            sizeBytes = stagedFile.length(),
                            direction = "UPLOAD",
                            status = "مرفوع في الرسائل المحفوظة (#$msgId)",
                            timestamp = System.currentTimeMillis(),
                            telegramMsgId = msgId
                        )
                    )

                    mainHandler.post {
                        listener?.onUploadFinished(entry)
                    }
                }
            } catch (t: Throwable) {
                notifyError("خطأ أثناء رفع الملف إلى الرسائل المحفوظة: ${t.localizedMessage ?: ""}")
            }
        }
    }

    fun deleteMessageFromSavedMessages(telegramMessageId: Long) {
        val targetChat = if (savedMessagesChatId != 0L) savedMessagesChatId else myUserId
        if (targetChat == 0L || telegramMessageId <= 0L) return
        val delReq = newTdObject("DeleteMessages").apply {
            setField(this, "chatId", targetChat)
            setField(this, "messageIds", longArrayOf(telegramMessageId))
            setField(this, "revoke", true)
        }
        sendTdRequest(delReq)
    }

    fun logoutFromTdlib() {
        try {
            val req = newTdObject("LogOut")
            sendTdRequest(req)
        } catch (_: Throwable) {}
    }

    // ============================================================
    // دوال الانعكاس الآمن (Reflection Helpers) للتوافق مع كافة إصدارات TDLib
    // ============================================================
    private fun newTdObject(simpleName: String): Any {
        val cls = Class.forName("$activePackagePrefix.TdApi\$$simpleName")
        for (ctor in cls.declaredConstructors) {
            if (ctor.parameterTypes.isEmpty()) {
                ctor.isAccessible = true
                return ctor.newInstance()
            }
        }
        val ctor = cls.declaredConstructors.first()
        ctor.isAccessible = true
        val defaultArgs = ctor.parameterTypes.map { pType ->
            when (pType) {
                Boolean::class.javaPrimitiveType, java.lang.Boolean::class.java -> false
                Int::class.javaPrimitiveType, java.lang.Integer::class.java -> 0
                Long::class.javaPrimitiveType, java.lang.Long::class.java -> 0L
                Double::class.javaPrimitiveType, java.lang.Double::class.java -> 0.0
                Float::class.javaPrimitiveType, java.lang.Float::class.java -> 0f
                String::class.java -> ""
                ByteArray::class.java -> ByteArray(0)
                LongArray::class.java -> LongArray(0)
                IntArray::class.java -> IntArray(0)
                else -> null
            }
        }.toTypedArray()
        return ctor.newInstance(*defaultArgs)
    }

    private fun findField(cls: Class<*>, name: String): java.lang.reflect.Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            try {
                val f = current.getDeclaredField(name)
                f.isAccessible = true
                return f
            } catch (_: Throwable) {
                current = current.superclass
            }
        }
        return null
    }

    private fun setField(target: Any, name: String, value: Any?) {
        try {
            val f = findField(target.javaClass, name) ?: return
            f.set(target, value)
        } catch (_: Throwable) {}
    }

    private fun getField(target: Any, name: String): Any? {
        return try {
            val f = findField(target.javaClass, name) ?: return null
            f.get(target)
        } catch (_: Throwable) {
            null
        }
    }

    private fun notifyStatus(status: String, detail: String) {
        mainHandler.post {
            listener?.onBridgeStatusChanged(status, detail)
        }
    }

    private fun notifyError(msg: String) {
        mainHandler.post {
            listener?.onError(msg)
        }
    }

    private fun translateTdlibError(code: Int, raw: String): String {
        return when {
            raw.contains("PHONE_NUMBER_INVALID", ignoreCase = true) ->
                "رقم الهاتف غير صحيح — تأكد من كتابته بمفتاح الدولة مثل +2011..."
            raw.contains("PHONE_CODE_INVALID", ignoreCase = true) ->
                "كود التفعيل غير صحيح — تأكد من آخر كود وصلك في تطبيق تيليجرام"
            raw.contains("PHONE_CODE_EXPIRED", ignoreCase = true) ->
                "انتهت صلاحية الكود — أعد طلب الكود من جديد"
            raw.contains("PASSWORD_HASH_INVALID", ignoreCase = true) ->
                "كلمة مرور التحقق بخطوتين (2FA) غير صحيحة"
            raw.contains("FLOOD_WAIT", ignoreCase = true) ->
                "محاولات كثيرة متتالية — يرجى الانتظار قليلاً قبل المحاولة مجدداً"
            else -> "رسالة تيليجرام ($code): $raw"
        }
    }
}
