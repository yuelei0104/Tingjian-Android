package com.tingjian.app

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
internal fun LiveScreen(large: Boolean, lines: SnapshotStateList<ChatLine>,
    language: String, onLanguageChange: (String) -> Unit, voiceMode: String,
    voiceStyle: String, ttsSpeed: Float, keywords: List<String>,
    keywordVibration: Boolean, keywordHighlight: Boolean,
    quickPhrases: List<QuickPhrase>, sessionStartedAt: Long, onFinish: () -> Unit) {
    val context = LocalContext.current
    var reply by remember { mutableStateOf("") }
    var confirmFinish by remember { mutableStateOf(false) }
    var quickOpen by remember { mutableStateOf(false) }
    var assistOpen by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf("") }
    var previousReply by remember { mutableStateOf("") }
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
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var recognitionGeneration by remember { mutableIntStateOf(0) }
    var showServiceHelp by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var playbackNotice by remember { mutableStateOf("") }
    var lastPlaybackText by remember { mutableStateOf("") }
    var showMicDisclosure by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf("已连接") }
    var keywordNotice by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    val keywordCooldown = remember { mutableMapOf<String, Long>() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        AlertDialog(onDismissRequest = { assistOpen = false },
            title = { Text("表达助手 · 界面预览") },
            text = {
                Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                    Text("当前使用本地模板预览完整交互，不会把输入发送到云端。" +
                        "翻译仅支持原型示例句，正式版本需接入 AI 服务。",
                        color = secondary, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    val source = reply.trim()
                    val choices = listOf(
                        "更礼貌" to if (source.endsWith("谢谢。")) source else "$source 谢谢。",
                        "更简洁" to source.substringBefore('，').substringBefore('。').plus("。"),
                        "更正式" to "我已了解相关内容：$source",
                        "译成中文" to if (source == "Okay, I will submit it on time.")
                            "好的，我会按时提交。" else "仅支持原型示例句翻译",
                        "译成英文" to if (source == "好的，我会按时提交。")
                            "Okay, I will submit it on time." else "Only the prototype sentence is supported."
                    )
                    choices.forEach { (label, candidate) ->
                        OutlinedButton(onClick = { suggestion = candidate },
                            modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                    if (suggestion.isNotEmpty()) {
                        Spacer(Modifier.height(9.dp))
                        Surface(color = mint, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(13.dp)) {
                                Text("候选建议", color = teal, fontSize = 12.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(suggestion, color = ink, fontSize = 14.sp)
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
                }, enabled = suggestion.isNotEmpty()) {
                    Text("采用建议", color = teal)
                }
            },
            dismissButton = {
                TextButton(onClick = { assistOpen = false; suggestion = "" }) {
                    Text("关闭")
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
                    recognizer?.cancel()
                    tts?.stop()
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
        tts?.stop()
        status = "已停止播放"
    }

    fun playText(text: String) {
        // 播音前取消本轮识别；旧识别回调不得覆盖播放状态或写入回声。
        recognitionGeneration++
        recognizer?.cancel()
        busy = false
        listening = false
        partial = ""
        playingId = null
        lastPlaybackText = text
        val engine = tts
        if (engine == null || !ttsReady) {
            playbackNotice = "无法播报：请在手机设置中安装并启用文字转语音引擎。"
            status = "文字已发送，播报不可用"
            return
        }
        val locale = when (voiceMode) {
            "中文" -> Locale.SIMPLIFIED_CHINESE
            "English" -> Locale.US
            else -> if (text.any { it in '\u4e00'..'\u9fff' }) Locale.SIMPLIFIED_CHINESE else Locale.US
        }
        if (engine.setLanguage(locale) < 0) {
            playbackNotice = "当前语音引擎缺少对应语言的语音数据。"
            status = "文字已发送，播报不可用"
            return
        }
        val styleRate = when (voiceStyle) { "清晰" -> 0.9f; "舒缓" -> 0.78f; else -> 1.0f }
        engine.setSpeechRate((styleRate * ttsSpeed).coerceIn(0.5f, 1.5f))
        engine.setPitch(when (voiceStyle) { "清晰" -> 1.03f; "舒缓" -> 0.96f; else -> 1.0f })
        playbackNotice = ""
        val id = System.nanoTime().toString()
        playingId = id
        status = "正在播报…"
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            playingId = null
            playbackNotice = "播报失败，请检查系统语音引擎后重试。"
            status = "文字已发送，播报失败"
        }
    }

    fun startRecognition() {
        if (!continuousListening || busy || playingId != null || paused) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            continuousListening = false
            status = "未找到语音识别服务。请检查设备是否安装并启用了语音识别服务。"
            showServiceHelp = true
            return
        }
        showServiceHelp = false
        try {
            val generation = ++recognitionGeneration
            val service = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
                recognizer = it
            }
            service.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (generation == recognitionGeneration) status = "持续监听中 · 请说话"
                }
                override fun onBeginningOfSpeech() {
                    if (generation == recognitionGeneration) status = "持续监听中 · 正在聆听"
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (generation != recognitionGeneration) return
                    listening = false; status = "正在生成字幕…"
                }
                override fun onError(error: Int) {
                    if (generation != recognitionGeneration) return
                    busy = false
                    listening = false
                    partial = ""
                    if (error == SpeechRecognizer.ERROR_NETWORK ||
                        error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) connectionState = "重连中"
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        permissionDenied = true
                        continuousListening = false
                    }
                    restartDelayMillis = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 250L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1500L
                        else -> 1000L
                    }
                    status = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "持续监听中…"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "持续监听中…"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "没有麦克风权限，请在系统设置中允许。"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "网络异常，正在自动重连…"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务正忙，正在自动重试…"
                        else -> "识别暂停（错误 $error），正在自动恢复…"
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (generation != recognitionGeneration) return
                    val recognized = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (recognized.isNotBlank()) {
                        lines.add(ChatLine(recognized, false))
                        connectionState = "已连接"
                        val hit = keywords.firstOrNull { recognized.contains(it, ignoreCase = true) }
                        if (hit != null) {
                            keywordNotice = "关键词提醒：$hit"
                            val now = System.currentTimeMillis()
                            val last = keywordCooldown[hit] ?: 0L
                            if (keywordVibration && now - last >= 10_000L) {
                                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                                } else {
                                    @Suppress("DEPRECATION")
                                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(100,
                                        VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(100)
                                }
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
                override fun onPartialResults(partialResults: Bundle?) {
                    if (generation != recognitionGeneration) return
                    partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                when (language) {
                    "中文" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    "English" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                }
            }
            partial = ""
            busy = true
            listening = true
            connectionState = "已连接"
            status = "正在启动持续监听…"
            service.startListening(request)
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
    DisposableEffect(context) {
        val main = Handler(Looper.getMainLooper())
        var disposed = false
        val engine = TextToSpeech(context) { result ->
            main.post {
                if (!disposed) {
                    ttsReady = result == TextToSpeech.SUCCESS
                    if (!ttsReady) playbackNotice = "系统语音引擎不可用，仍可发送文字。"
                }
            }
        }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) {
                main.post {
                    if (!disposed && playingId == utteranceId) {
                        playingId = null
                        status = "播报完成"
                    }
                }
            }
            override fun onError(utteranceId: String) {
                main.post {
                    if (!disposed && playingId == utteranceId) {
                        playingId = null
                        playbackNotice = "播报失败，请检查系统语音引擎。"
                        status = "播报失败"
                    }
                }
            }
        })
        onDispose {
            disposed = true
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recognitionGeneration++
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        }
    }
    DisposableEffect(context) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && (continuousListening || listening || busy)) {
                recognitionGeneration++
                recognizer?.cancel()
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

    Column(Modifier.fillMaxSize().imePadding().background(canvas)) {
        Column(Modifier.fillMaxWidth().background(white)
            .padding(horizontal = 20.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("面对面字幕", color = ink, fontSize = 21.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(String.format(Locale.ROOT, "%02d:%02d", elapsedSeconds / 60,
                    elapsedSeconds % 60), color = secondary, fontSize = 12.sp)
                TextButton(onClick = { confirmFinish = true }, enabled = lines.isNotEmpty()) {
                    Text("结束并保存", color = if (lines.isNotEmpty()) teal else secondary,
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
                    modifier = Modifier.weight(1f))
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
                        recognizer?.cancel()
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
                    Text(keywordNotice, color = ink, fontSize = 12.sp,
                        modifier = Modifier.weight(1f).background(Color(0xFFFFF2C7),
                            RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
                } else Spacer(Modifier.weight(1f))
                TextButton(onClick = { autoScroll = !autoScroll }) {
                    Text(if (autoScroll) "跟随字幕" else "停止跟随", color = teal, fontSize = 12.sp)
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
                    keywords = if (keywordHighlight) keywords else emptyList())
                Spacer(Modifier.height(12.dp))
            }
            if (partial.isNotBlank()) {
                ChatBubble(partial, fromMe = false, large = large, inProgress = true)
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
                        recognizer?.cancel()
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
                            lines.add(ChatLine(message, true))
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
                    else { suggestion = ""; assistOpen = true }
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
