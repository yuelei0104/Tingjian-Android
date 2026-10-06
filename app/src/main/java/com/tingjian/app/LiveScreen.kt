package com.tingjian.app

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme
import com.tingjian.app.data.ApiResult
import com.tingjian.app.network.AiSuggestionRequest
import com.tingjian.app.network.AiSuggestionResponse
import com.tingjian.app.network.NetworkModule
import com.tingjian.app.speech.AndroidSpeechSynthesisProvider
import com.tingjian.app.speech.RecognitionFailureKind
import com.tingjian.app.speech.SpeechRecognitionProvider
import com.tingjian.app.speech.SpeechStartResult
import com.tingjian.app.speech.SpeechSynthesisProvider
import com.tingjian.app.speech.SpeechSynthesisRequest
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
internal fun LiveScreen(large: Boolean, lines: SnapshotStateList<ChatLine>,
    language: String, onLanguageChange: (String) -> Unit, voiceMode: String,
    voiceStyle: String, ttsSpeed: Float, keywords: List<String>,
    keywordVibration: Boolean, keywordHighlight: Boolean,
    highContrast: Boolean, visualAlerts: Boolean, systemNotifications: Boolean,
    strongVibration: Boolean, captionFollow: Boolean,
    quickPhrases: List<QuickPhrase>, sessionStartedAt: Long,
    cloudSyncState: String, pendingMessageCount: Int,
    aiLoggedIn: Boolean, aiSessionId: String?,
    onGenerateSuggestion: suspend (AiSuggestionRequest) -> ApiResult<AiSuggestionResponse>,
    onRetryCloudSync: () -> Unit,
    onLineAdded: (ChatLine, Int) -> Unit,
    onFinish: () -> Unit) {
    val context = LocalContext.current
    val accessibilityFeedback = remember { AccessibilityFeedback(context) }
    var reply by remember { mutableStateOf("") }
    var confirmFinish by remember { mutableStateOf(false) }
    var quickOpen by remember { mutableStateOf(false) }
    var assistOpen by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf("") }
    var previousReply by remember { mutableStateOf("") }
    var assistLoading by remember { mutableStateOf(false) }
    var assistError by remember { mutableStateOf("") }
    var assistProvider by remember { mutableStateOf("") }
    var lastAssistRequest by remember { mutableStateOf<AiSuggestionRequest?>(null) }
    var assistJob by remember { mutableStateOf<Job?>(null) }
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("等待开始") }
    var listening by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var continuousListening by remember { mutableStateOf(false) }
    var restartDelayMillis by remember { mutableLongStateOf(300L) }
    var paused by remember { mutableStateOf(false) }
    var pausedAt by remember { mutableLongStateOf(0L) }
    var totalPausedMillis by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember(sessionStartedAt) { mutableLongStateOf(0L) }
    var recognitionGeneration by remember { mutableIntStateOf(0) }
    var showServiceHelp by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var playbackNotice by remember { mutableStateOf("") }
    var lastPlaybackText by remember { mutableStateOf("") }
    var showMicDisclosure by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf("已连接") }
    var keywordNotice by remember { mutableStateOf("") }
    var autoScroll by remember(captionFollow) { mutableStateOf(captionFollow) }
    var previousCloudSyncState by remember { mutableStateOf(cloudSyncState) }
    val keywordCooldown = remember { mutableMapOf<String, Long>() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val recognitionProvider = remember(context) { NetworkModule.speechRecognitionProvider(context) }
    val synthesisProvider = remember(context) { AndroidSpeechSynthesisProvider(context) }

    fun generateSuggestion(action: String, retryRequest: AiSuggestionRequest? = null) {
        val request = retryRequest ?: AiSuggestionRequest(
            clientRequestId = UUID.randomUUID().toString(),
            sessionId = aiSessionId,
            sourceText = reply.trim(),
            action = action,
            language = language,
            context = buildAiContext(lines)
        )
        lastAssistRequest = request
        assistJob?.cancel()
        assistJob = scope.launch {
            assistLoading = true
            assistError = ""
            assistProvider = ""
            if (!aiLoggedIn) {
                suggestion = localExpressionSuggestion(request.sourceText, request.action, request.context)
                assistProvider = "本机模板"
                assistError = "登录后可使用服务端表达助手；当前结果仅在本机生成。"
                assistLoading = false
                return@launch
            }
            when (val result = onGenerateSuggestion(request)) {
                is ApiResult.Success -> {
                    suggestion = result.value.suggestion
                    assistProvider = if (result.value.fallback) "本机安全回退" else result.value.provider
                }
                is ApiResult.Error -> {
                    suggestion = localExpressionSuggestion(request.sourceText, request.action, request.context)
                    assistProvider = "本机安全回退"
                    assistError = "${result.message}；已提供本机结果，可重试云端。"
                }
            }
            assistLoading = false
        }
    }

    LaunchedEffect(cloudSyncState, systemNotifications) {
        if (previousCloudSyncState == "已连接" && cloudSyncState != "已连接") {
            accessibilityFeedback.connectionLost(systemNotifications)
        }
        previousCloudSyncState = cloudSyncState
    }

    LaunchedEffect(sessionStartedAt, paused) {
        while (true) {
            if (!paused && sessionStartedAt > 0L) {
                elapsedSeconds = (System.currentTimeMillis() - sessionStartedAt - totalPausedMillis)
                    .coerceAtLeast(0L) / 1000L
            }
            delay(1000)
        }
    }
    LaunchedEffect(keywordNotice) {
        if (keywordNotice.isNotEmpty()) {
            delay(4000)
            keywordNotice = ""
        }
    }

    if (quickOpen) {
        AlertDialog(onDismissRequest = { quickOpen = false },
            title = { Text("常用回复") },
            text = {
                Column {
                    quickPhrases.ifEmpty { listOf(QuickPhrase("暂无可用快捷短语")) }
                        .forEach { phrase ->
                        TextButton(onClick = {
                            if (quickPhrases.isNotEmpty()) reply = phrase.text
                            quickOpen = false
                        }, enabled = quickPhrases.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(phrase.text, color = ink, textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { quickOpen = false }) { Text("关闭", color = teal) }
            })
    }

    if (assistOpen) {
        AlertDialog(onDismissRequest = {
            assistJob?.cancel()
            assistLoading = false
            assistOpen = false
        },
            title = { Text("表达助手") },
            text = {
                Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                    Text("选择处理方式后生成候选内容。采用建议只会填入输入框，不会自动发送或播报。",
                        color = secondary, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    val choices = listOf(
                        "结合对话回复" to AiAction.REPLY,
                        "更礼貌" to AiAction.POLITE,
                        "更简洁" to AiAction.CONCISE,
                        "更正式" to AiAction.FORMAL,
                        "译成中文" to AiAction.TRANSLATE_ZH,
                        "译成英文" to AiAction.TRANSLATE_EN
                    )
                    choices.forEach { (label, action) ->
                        OutlinedButton(onClick = { generateSuggestion(action) },
                            enabled = !assistLoading,
                            modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                    if (assistLoading) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(9.dp))
                            Text("正在生成候选建议…", color = secondary, fontSize = 13.sp)
                        }
                    }
                    if (assistError.isNotEmpty()) {
                        Text(assistError, color = secondary, fontSize = 12.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        TextButton(onClick = {
                            lastAssistRequest?.let { generateSuggestion(it.action, it) }
                        }, enabled = !assistLoading && lastAssistRequest != null) {
                            Text("重试", color = teal)
                        }
                    }
                    if (suggestion.isNotEmpty()) {
                        Spacer(Modifier.height(9.dp))
                        Surface(color = mint, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(13.dp)) {
                                Text("候选建议", color = teal, fontSize = 12.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(suggestion, color = ink, fontSize = 14.sp)
                                if (assistProvider.isNotEmpty()) {
                                    Spacer(Modifier.height(5.dp))
                                    Text("来源：$assistProvider", color = secondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    previousReply = reply
                    reply = suggestion
                    suggestion = ""
                    assistOpen = false
                }, enabled = suggestion.isNotEmpty() && !assistLoading) {
                    Text("采用建议", color = teal)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (assistLoading) {
                        assistJob?.cancel()
                        assistLoading = false
                        assistError = "已取消生成。"
                    } else {
                        assistOpen = false
                        suggestion = ""
                        assistError = ""
                    }
                }) {
                    Text(if (assistLoading) "取消生成" else "关闭")
                }
            })
    }
    if (confirmFinish) {
        AlertDialog(onDismissRequest = { confirmFinish = false },
            title = { Text("结束会话？") },
            text = { Text("当前 ${lines.size} 条消息将保存在这台设备的记录页。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    continuousListening = false
                    recognitionGeneration++
                    recognitionProvider.cancel()
                    synthesisProvider.stop()
                    playingId = null
                    onFinish()
                }) { Text("结束并保存", color = teal) }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text("继续会话") }
            })
    }

    fun stopPlayback() {
        playingId = null
        synthesisProvider.stop()
        status = "已停止播放"
    }

    fun playText(text: String) {
        // 播音前取消本轮识别；旧识别回调不得覆盖播放状态或写入回声。
        recognitionGeneration++
        recognitionProvider.cancel()
        busy = false
        listening = false
        partial = ""
        playingId = null
        lastPlaybackText = text
        if (!ttsReady) {
            playbackNotice = "无法播报：请在手机设置中安装并启用文字转语音引擎。"
            status = "文字已发送，播报不可用"
            return
        }
        playbackNotice = ""
        val result = synthesisProvider.speak(
            SpeechSynthesisRequest(text, voiceMode, voiceStyle, ttsSpeed),
            object : SpeechSynthesisProvider.Listener {
                override fun onStarted(utteranceId: String) {
                    if (playingId == utteranceId) status = "正在播报…"
                }

                override fun onCompleted(utteranceId: String) {
                    if (playingId == utteranceId) {
                        playingId = null
                        status = "播报完成"
                    }
                }

                override fun onFailure(utteranceId: String, message: String) {
                    if (playingId == utteranceId) {
                        playingId = null
                        playbackNotice = message
                        status = "播报失败"
                    }
                }
            })
        when (result) {
            is SpeechStartResult.Started -> {
                playingId = result.utteranceId
                status = "正在播报…"
            }
            is SpeechStartResult.Error -> {
                playbackNotice = result.message
                status = "文字已发送，播报不可用"
            }
        }
    }

    fun startRecognition() {
        if (!continuousListening || busy || playingId != null || paused) return
        if (!recognitionProvider.isAvailable()) {
            continuousListening = false
            status = "未找到语音识别服务。请检查设备是否安装并启用了语音识别服务。"
            showServiceHelp = true
            return
        }
        showServiceHelp = false
        try {
            val generation = ++recognitionGeneration
            partial = ""
            busy = true
            listening = true
            connectionState = "已连接"
            status = "正在启动持续监听…"
            recognitionProvider.start(language, object : SpeechRecognitionProvider.Listener {
                override fun onReady() {
                    if (generation == recognitionGeneration) status = "持续监听中 · 请说话"
                }
                override fun onSpeechStarted() {
                    if (generation == recognitionGeneration) status = "持续监听中 · 正在聆听"
                }
                override fun onSpeechEnded() {
                    if (generation != recognitionGeneration) return
                    listening = false; status = "正在生成字幕…"
                }
                override fun onFailure(failure: com.tingjian.app.speech.RecognitionFailure) {
                    if (generation != recognitionGeneration) return
                    busy = false
                    listening = false
                    partial = ""
                    if (failure.kind == RecognitionFailureKind.NETWORK) connectionState = "重连中"
                    if (failure.kind == RecognitionFailureKind.PERMISSION) {
                        permissionDenied = true
                        continuousListening = false
                    }
                    restartDelayMillis = failure.retryDelayMillis
                    status = failure.message
                }
                override fun onResult(text: String) {
                    if (generation != recognitionGeneration) return
                    val recognized = text
                    if (recognized.isNotBlank()) {
                        val line = ChatLine(recognized, false)
                        lines.add(line)
                        onLineAdded(line, lines.lastIndex)
                        connectionState = "已连接"
                        val hit = keywords.firstOrNull { recognized.contains(it, ignoreCase = true) }
                        if (hit != null) {
                            keywordNotice = if (visualAlerts) "关键词提醒：$hit" else ""
                            val now = System.currentTimeMillis()
                            val last = keywordCooldown[hit] ?: 0L
                            if (now - last >= 10_000L) {
                                accessibilityFeedback.keyword(
                                    keyword = hit,
                                    vibrationEnabled = keywordVibration,
                                    strongVibration = strongVibration,
                                    notificationEnabled = systemNotifications
                                )
                                keywordCooldown[hit] = now
                            }
                        }
                    }
                    partial = ""
                    busy = false
                    listening = false
                    restartDelayMillis = 250L
                    status = if (recognized.isBlank()) "持续监听中…"
                        else "字幕已生成，继续监听中…"
                }
                override fun onPartial(text: String) {
                    if (generation != recognitionGeneration) return
                    partial = text
                }
            })
        } catch (e: Exception) {
            busy = false
            listening = false
            restartDelayMillis = 1500L
            connectionState = "连接失败"
            status = "无法启动识别，正在自动重试…"
        }
    }

    // Android 的系统识别器每次停顿后都会结束一轮。只要用户没有停止，
    // 就在本轮结果、超时或可恢复错误后自动开启下一轮。
    LaunchedEffect(continuousListening, paused, playingId, busy, listening, restartDelayMillis) {
        if (continuousListening && !paused && playingId == null && !busy && !listening) {
            delay(restartDelayMillis)
            if (continuousListening && !paused && playingId == null && !busy && !listening) {
                startRecognition()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permissionDenied = false
            startRecognition()
        } else {
            continuousListening = false
            permissionDenied = true
            status = "麦克风权限未授予；可在系统设置中为听见开启权限。"
        }
    }
    if (showMicDisclosure) {
        AlertDialog(onDismissRequest = {
            showMicDisclosure = false
            continuousListening = false
        },
            title = { Text("使用麦克风") },
            text = { Text("听见会在你开始持续监听后使用麦克风，直到你手动停止、暂停或结束会话。默认不保存原始录音；" +
                "设备语音服务是否联网以及如何处理音频，取决于设备服务商。") },
            confirmButton = {
                TextButton(onClick = {
                    showMicDisclosure = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("继续授权", color = teal) }
            }, dismissButton = {
                TextButton(onClick = {
                    showMicDisclosure = false
                    continuousListening = false
                }) { Text("暂不使用") }
            })
    }
    DisposableEffect(synthesisProvider) {
        var disposed = false
        synthesisProvider.initialize { ready ->
            if (!disposed) {
                ttsReady = ready
                if (!ready) playbackNotice = "系统语音引擎不可用，仍可发送文字。"
            }
        }
        onDispose {
            disposed = true
            synthesisProvider.release()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recognitionGeneration++
            recognitionProvider.release()
        }
    }
    DisposableEffect(context) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (continuousListening || listening || busy)) {
                recognitionGeneration++
                recognitionProvider.cancel()
                listening = false
                busy = false
                partial = ""
                if (!paused) pausedAt = System.currentTimeMillis()
                paused = true
                status = "应用进入后台，已自动暂停"
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    val scroll = rememberScrollState()
    LaunchedEffect(lines.size, partial, autoScroll) {
        if (autoScroll) scroll.animateScrollTo(scroll.maxValue)
    }

    Column(Modifier.fillMaxSize().imePadding()
        .background(if (highContrast) Color.White else canvas)) {
        Column(Modifier.fillMaxWidth().background(white)
            .padding(horizontal = 20.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("面对面字幕", color = ink, fontSize = 21.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(String.format(Locale.ROOT, "%02d:%02d", elapsedSeconds / 60,
                    elapsedSeconds % 60), color = secondary, fontSize = 12.sp)
                val canFinish = lines.isNotEmpty() || sessionStartedAt > 0L
                TextButton(onClick = { confirmFinish = true }, enabled = canFinish) {
                    Text("结束并保存", color = if (canFinish) teal else secondary,
                        fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("中英混合", "中文", "English").forEach { option ->
                    Surface(shape = RoundedCornerShape(50),
                        color = if (language == option) mint else white,
                        border = BorderStroke(1.dp, if (language == option) teal else divider),
                        modifier = Modifier.clickable(enabled = !continuousListening && !busy) {
                            onLanguageChange(option)
                        }) {
                        Text(option, Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            fontSize = 12.sp, color = if (language == option) teal else secondary)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$connectionState · ${if (paused) "已暂停识别" else status}", color = teal,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics {
                        liveRegion = LiveRegionMode.Polite
                    })
                if (connectionState != "已连接") {
                    TextButton(onClick = {
                        connectionState = "已连接"
                        status = "连接已恢复，可继续识别"
                    }) { Text("重试连接", color = teal, fontSize = 12.sp) }
                }
                TextButton(onClick = {
                    val willPause = !paused
                    if (willPause) {
                        pausedAt = System.currentTimeMillis()
                        recognitionGeneration++
                        recognitionProvider.cancel()
                        listening = false
                        busy = false
                        partial = ""
                        status = "已暂停持续监听"
                    } else {
                        if (pausedAt > 0L) totalPausedMillis +=
                            (System.currentTimeMillis() - pausedAt).coerceAtLeast(0L)
                        pausedAt = 0L
                        status = if (continuousListening) "已继续，正在恢复持续监听…"
                            else "已继续，可开始监听"
                    }
                    paused = willPause
                }) { Text(if (paused) "继续" else "暂停", color = teal, fontSize = 11.sp) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (keywordNotice.isNotEmpty()) {
                    Text(keywordNotice,
                        color = if (highContrast) Color.Black else ink,
                        fontSize = if (large) 16.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).background(
                            if (highContrast) Color(0xFFFFFF00) else Color(0xFFFFF2C7),
                            RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
                } else Spacer(Modifier.weight(1f))
                TextButton(onClick = { autoScroll = !autoScroll }) {
                    Text(if (autoScroll) "跟随字幕" else "停止跟随", color = teal, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("云端同步：$cloudSyncState" +
                    if (pendingMessageCount > 0) " · 待发送 $pendingMessageCount 条" else "",
                    color = if (pendingMessageCount > 0) secondary else teal,
                    fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (cloudSyncState != "已连接" || pendingMessageCount > 0) {
                    TextButton(onClick = onRetryCloudSync) {
                        Text("重试同步", color = teal, fontSize = 11.sp)
                    }
                }
            }
        }
        HorizontalDivider(color = divider)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 16.dp)) {
            if (lines.isEmpty() && partial.isBlank()) {
                Spacer(Modifier.height(34.dp))
                Text("对方说的话会显示在左侧。\n你的文字会显示在右侧。",
                    modifier = Modifier.fillMaxWidth(), color = secondary,
                    fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            }
            lines.forEach { line ->
                ChatBubble(line.content, line.fromMe, large,
                    onReplay = if (line.fromMe) ({ playText(line.content) }) else null,
                    keywords = if (keywordHighlight) keywords else emptyList(),
                    highContrast = highContrast)
                Spacer(Modifier.height(12.dp))
            }
            if (partial.isNotBlank()) {
                ChatBubble(partial, fromMe = false, large = large, inProgress = true,
                    highContrast = highContrast)
                Spacer(Modifier.height(12.dp))
            }
            if (showServiceHelp) {
                Column {
                    Text("当前手机没有可用的系统语音识别服务，仍可输入文字交流。",
                        color = secondary, fontSize = 14.sp, lineHeight = 20.sp)
                    TextButton(onClick = { showServiceHelp = false; status = "等待重试" }) {
                        Text("重新检测", color = teal)
                    }
                }
            }
            if (!autoScroll && lines.isNotEmpty()) {
                TextButton(onClick = { autoScroll = true }) {
                    Text("回到最新字幕 ↓", color = teal)
                }
            }
        }
        Surface(color = white, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (continuousListening) {
                        continuousListening = false
                        recognitionGeneration++
                        recognitionProvider.cancel()
                        listening = false
                        busy = false
                        partial = ""
                        status = "持续监听已停止"
                    } else if (!busy) {
                        continuousListening = true
                        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            startRecognition()
                        else showMicDisclosure = true
                    }
                }, enabled = !paused && (playingId == null || continuousListening),
                    modifier = Modifier.height(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                    Text(if (continuousListening) "停止监听" else "开始监听",
                        fontSize = 12.sp)
                }
                OutlinedTextField(value = reply, onValueChange = { reply = it.take(200) },
                    modifier = Modifier.weight(1f), minLines = 1, maxLines = 3,
                    placeholder = { Text("输入文字…", fontSize = 15.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = teal, unfocusedBorderColor = divider,
                        focusedContainerColor = white, unfocusedContainerColor = white))
                TextButton(onClick = {
                    if (playingId != null) {
                        stopPlayback()
                    } else {
                        val message = reply.trim()
                        if (message.isNotEmpty()) {
                            val line = ChatLine(message, true)
                            lines.add(line)
                            onLineAdded(line, lines.lastIndex)
                            playText(message)
                        }
                        reply = ""
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }, enabled = reply.isNotBlank() || playingId != null,
                    contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(if (playingId != null) "停止" else "发送并播报",
                        color = if (reply.isNotBlank() || playingId != null) teal else secondary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End) {
                if (previousReply.isNotEmpty()) {
                    TextButton(onClick = {
                        reply = previousReply
                        previousReply = ""
                    }) { Text("撤销建议", color = secondary, fontSize = 11.sp) }
                }
                TextButton(onClick = {
                    if (reply.isBlank()) quickOpen = true
                    else {
                        suggestion = ""
                        assistError = ""
                        assistProvider = ""
                        lastAssistRequest = null
                        assistOpen = true
                    }
                }) {
                    Text(if (reply.isBlank()) "常用回复" else "表达助手",
                        color = teal, fontSize = 11.sp)
                }
            }
            }
        }
        if (playbackNotice.isNotEmpty() || permissionDenied) {
            Row(Modifier.fillMaxWidth().background(white)
                .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (permissionDenied) "麦克风权限未开启" else playbackNotice,
                    color = secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (permissionDenied) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")))
                    }) { Text("打开设置", color = teal) }
                } else if (lastPlaybackText.isNotEmpty()) {
                    TextButton(onClick = { playText(lastPlaybackText) }) {
                        Text("重试播报", color = teal)
                    }
                }
            }
        }
    }
}
