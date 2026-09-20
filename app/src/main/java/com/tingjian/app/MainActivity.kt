package com.tingjian.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme
import java.util.Locale

// 历史记录仍为本地示例；字幕页使用设备上的 Android 语音识别服务。
private val ink = Color(0xFF173138)
private val secondary = Color(0xFF60777D)
private val teal = Color(0xFF147D78)
private val deep = Color(0xFF163E41)
private val mint = Color(0xFFE7F6F1)
private val canvas = Color(0xFFF7FAF9)
private val divider = Color(0xFFE1EBE8)
private val white = Color(0xFFFFFFFF)

private data class Conversation(
    val title: String, val time: String, val preview: String, val duration: String,
    val transcript: List<Pair<String, String>>
)

private data class ChatLine(val content: String, val fromMe: Boolean)

private val examples = listOf(
    Conversation("和朋友聊聊周末", "今天 14:32", "我们周六去公园走走，怎么样？", "12 分钟",
        listOf("对方" to "我们周六去公园走走，怎么样？", "我" to "好呀，下午见！")),
    Conversation("课堂笔记 · 人工智能", "昨天 09:15", "老师说，模型的训练需要关注数据质量。", "48 分钟",
        listOf("老师" to "模型的训练需要关注数据质量。", "老师" to "下周请提交课程报告。")),
    Conversation("家庭聚餐", "9 月 17 日", "下次见面一起做你最喜欢的菜。", "26 分钟",
        listOf("家人" to "下次见面一起做你最喜欢的菜。", "我" to "好，我很期待。"))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TingjianTheme(dynamicColor = false) { TingjianApp() } }
    }
}

@Composable
private fun TingjianApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("tingjian_display", android.content.Context.MODE_PRIVATE) }
    var entered by remember { mutableStateOf(preferences.getBoolean("welcome_completed", false)) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Conversation?>(null) }
    var large by remember { mutableStateOf(preferences.getBoolean("large_text", false)) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = entered && selected != null) { selected = null }
    BackHandler(enabled = entered && selected == null && tab != 0) { tab = 0 }

    if (!entered) {
        WelcomeScreen {
            entered = true
            preferences.edit().putBoolean("welcome_completed", true).apply()
        }
        return
    }
    Scaffold(containerColor = canvas, bottomBar = {
        if (selected == null && !keyboardVisible) BottomTabs(tab) { tab = it }
    }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            val record = selected
            if (record != null) {
                DetailScreen(record, large) { selected = null }
            } else when (tab) {
                0 -> HomeScreen(onNew = { tab = 1 }, onHistory = { tab = 2 },
                    onOpen = { selected = it })
                1 -> LiveScreen(large)
                2 -> HistoryScreen(onOpen = { selected = it })
                else -> ProfileScreen(large, onLargeChange = {
                    large = it
                    preferences.edit().putBoolean("large_text", it).apply()
                },
                    onLeave = {
                        entered = false
                        tab = 0
                        preferences.edit().putBoolean("welcome_completed", false).apply()
                    })
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(teal, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            Text("听", fontSize = 27.sp, color = white, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Text("听见", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ink)
    }
}

@Composable
private fun Title(text: String, subtitle: String? = null) {
    Text(text, color = ink, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
    if (subtitle != null) {
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = secondary, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun Pill(text: String, highlighted: Boolean = false) {
    Surface(color = if (highlighted) mint else white, shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (highlighted) mint else divider)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (highlighted) teal else secondary)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = white)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WelcomeScreen(onEnter: () -> Unit) {
    Column(Modifier.fillMaxSize().background(canvas).verticalScroll(rememberScrollState())
        .padding(horizontal = 26.dp)) {
        Spacer(Modifier.height(58.dp))
        BrandMark()
        Spacer(Modifier.height(55.dp))
        Pill("让交流更自由", highlighted = true)
        Spacer(Modifier.height(17.dp))
        Text("听见每一句话。", color = ink, fontSize = 34.sp,
            lineHeight = 43.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("让交流更轻松，让陪伴更贴近。", color = secondary, fontSize = 16.sp)
        Spacer(Modifier.height(48.dp))
        Surface(color = white, shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(23.dp)) {
                Box(Modifier.size(45.dp).background(mint, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("≋", fontSize = 26.sp, color = teal)
                }
                Spacer(Modifier.height(18.dp))
                Text("欢迎使用听见", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ink)
                Spacer(Modifier.height(9.dp))
                Text("试用单句语音转文字和文字回复。记录页提供示例会话。",
                    fontSize = 14.sp, lineHeight = 22.sp, color = secondary)
                Spacer(Modifier.height(22.dp))
                PrimaryButton("开始体验  →", onEnter)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("语音识别需设备提供识别服务并授权麦克风。示例记录不会上传。",
            modifier = Modifier.fillMaxWidth(), color = secondary, fontSize = 12.sp,
            lineHeight = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(42.dp))
        Text("听见 Tingjian  ·  让每一种交流都有回响", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeScreen(onNew: () -> Unit, onHistory: () -> Unit, onOpen: (Conversation) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(24.dp))
        BrandMark()
        Spacer(Modifier.height(34.dp))
        Title("你好，今天也要好好听。", "让重要的话，都被看见。")
        Spacer(Modifier.height(27.dp))
        Surface(shape = RoundedCornerShape(26.dp), color = deep,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text("✦  实时字幕", fontSize = 13.sp, color = Color(0xFFB7E9DE))
                Spacer(Modifier.height(17.dp))
                Text("面对面交流，\n每一句都清晰。", fontSize = 26.sp,
                    lineHeight = 35.sp, fontWeight = FontWeight.Bold, color = white)
                Spacer(Modifier.height(23.dp))
                Button(onClick = onNew, shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDF8EF),
                        contentColor = deep)) {
                    Text("开始新会话   →", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("单句识别  ·  大字展示  ·  文字回复", fontSize = 12.sp, color = Color(0xFFB7E9DE))
            }
        }
        Spacer(Modifier.height(29.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("最近的会话", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onHistory) { Text("查看全部  →", color = teal, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(9.dp))
        examples.take(2).forEach { ConversationCard(it) { onOpen(it) }; Spacer(Modifier.height(11.dp)) }
        Text("会话卡片为示例内容，尚未连接真实记录。", fontSize = 12.sp, color = secondary)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConversationCard(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = white, shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, divider)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(mint, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) { Text("≋", color = teal, fontSize = 26.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
                Spacer(Modifier.height(5.dp))
                Text(item.preview, color = secondary, fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text("${item.time}  ·  ${item.duration}", color = secondary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text("示例", color = teal, fontSize = 11.sp,
                modifier = Modifier.background(mint, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.width(5.dp))
            Text("›", color = secondary, fontSize = 25.sp)
        }
    }
}

@Composable
private fun LiveScreen(large: Boolean) {
    val context = LocalContext.current
    var reply by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<ChatLine>() }
    var language by remember { mutableStateOf("中文") }
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("等待开始") }
    var listening by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var recognitionGeneration by remember { mutableIntStateOf(0) }
    var showServiceHelp by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var playbackNotice by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        val engine = tts
        if (engine == null || !ttsReady) {
            playbackNotice = "无法播报：请在手机设置中安装并启用文字转语音引擎。"
            status = "文字已发送，播报不可用"
            return
        }
        val locale = if (text.any { it in '\u4e00'..'\u9fff' }) Locale.SIMPLIFIED_CHINESE else Locale.US
        if (engine.setLanguage(locale) < 0) {
            playbackNotice = "当前语音引擎缺少对应语言的语音数据。"
            status = "文字已发送，播报不可用"
            return
        }
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
        if (busy || playingId != null) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
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
                    if (generation == recognitionGeneration) status = "请说话"
                }
                override fun onBeginningOfSpeech() {
                    if (generation == recognitionGeneration) status = "正在聆听"
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (generation != recognitionGeneration) return
                    listening = false; status = "正在识别…"
                }
                override fun onError(error: Int) {
                    if (generation != recognitionGeneration) return
                    busy = false
                    listening = false
                    partial = ""
                    status = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "没听清楚，请再试一次。"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有听到声音，请再试一次。"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "没有麦克风权限，请在系统设置中允许。"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "识别服务网络不可用，请检查网络后重试。"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务正忙，请稍后重试。"
                        else -> "识别失败（错误 $error），请重试。"
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (generation != recognitionGeneration) return
                    val recognized = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (recognized.isNotBlank()) lines.add(ChatLine(recognized, false))
                    partial = ""
                    busy = false
                    listening = false
                    status = if (recognized.isBlank()) "没有识别到文字，请重试。" else "✓  识别完成"
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == "中文") "zh-CN" else "en-US")
            }
            partial = ""
            busy = true
            listening = true
            status = "正在启动识别…"
            service.startListening(request)
        } catch (e: Exception) {
            busy = false
            listening = false
            status = "无法启动语音识别，请检查设备识别服务后重试。"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition() else status = "麦克风权限未授予；可在系统设置中为听见开启权限。"
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

    val scroll = rememberScrollState()
    LaunchedEffect(lines.size, partial) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().imePadding().background(canvas)) {
        Column(Modifier.fillMaxWidth().background(white)
            .padding(horizontal = 20.dp, vertical = 13.dp)) {
            Text("面对面字幕", color = ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("中文", "English").forEach { option ->
                    Surface(shape = RoundedCornerShape(50),
                        color = if (language == option) mint else white,
                        border = BorderStroke(1.dp, if (language == option) teal else divider),
                        modifier = Modifier.clickable(enabled = !busy) { language = option }) {
                        Text(option, Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            fontSize = 12.sp, color = if (language == option) teal else secondary)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(status, color = teal,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 124.dp))
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
                ChatBubble(line.content, line.fromMe, large)
                Spacer(Modifier.height(12.dp))
            }
            if (partial.isNotBlank()) {
                ChatBubble(partial, fromMe = false, large = large, inProgress = true)
                Spacer(Modifier.height(12.dp))
            }
            if (showServiceHelp) {
                Text("当前手机没有可用的系统语音识别服务，仍可输入文字交流。",
                    color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        Surface(color = white, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (listening) {
                        listening = false
                        status = "正在识别…"
                        recognizer?.stopListening()
                    } else if (!busy) {
                        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            startRecognition()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }, enabled = playingId == null && (!busy || listening),
                    modifier = Modifier.height(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                    Text(if (listening) "结束" else if (busy) "识别中" else "语音",
                        fontSize = 13.sp)
                }
                OutlinedTextField(value = reply, onValueChange = { reply = it.take(200) },
                    modifier = Modifier.weight(1f), minLines = 1, maxLines = 3,
                    placeholder = { Text("输入文字…", fontSize = 13.sp) },
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
        }
        if (playbackNotice.isNotEmpty()) {
            Text(playbackNotice, color = secondary, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(white)
                    .padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromMe: Boolean, large: Boolean,
    inProgress: Boolean = false, speaker: String? = null) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start) {
        Column(Modifier.fillMaxWidth(0.84f),
            horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start) {
            Text(speaker ?: if (fromMe) "我" else if (inProgress) "正在识别" else "对方",
                color = secondary, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp))
            Surface(color = if (fromMe) mint else white,
                shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, divider)) {
                Text(text, Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    color = ink, fontSize = if (large) 23.sp else 17.sp,
                    lineHeight = if (large) 34.sp else 26.sp)
            }
        }
    }
}

@Composable
private fun HistoryScreen(onOpen: (Conversation) -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = examples.filter {
        it.title.contains(search, ignoreCase = true) ||
            it.preview.contains(search, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(27.dp))
        Title("会话记录", "重要的对话，随时回来看看。")
        Spacer(Modifier.height(23.dp))
        OutlinedTextField(value = search, onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("搜索示例会话") }, shape = RoundedCornerShape(16.dp),
            trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) {
                Text("清除", fontSize = 12.sp, color = teal)
            } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = teal, unfocusedBorderColor = divider,
                focusedContainerColor = white, unfocusedContainerColor = white))
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("全部记录", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("演示记录 · ${filtered.size} 条", color = secondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(15.dp))
        if (filtered.isEmpty()) {
            Surface(color = white, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
                Text("没有找到相关会话", Modifier.padding(30.dp),
                    color = secondary, textAlign = TextAlign.Center)
            }
        }
        filtered.forEach { ConversationCard(it) { onOpen(it) }; Spacer(Modifier.height(11.dp)) }
        Spacer(Modifier.height(10.dp))
        Text("目前只展示本地示例，尚未保存真实会话。", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun DetailScreen(record: Conversation, large: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(16.dp))
        Title(record.title, "${record.time}  ·  ${record.duration}  ·  示例会话")
        Spacer(Modifier.height(29.dp))
        Text("示例对话", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        record.transcript.forEach { (speaker, content) ->
            ChatBubble(content, fromMe = speaker == "我", large = large, speaker = speaker)
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("以上内容仅供界面体验，不是真实识别结果。", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ProfileScreen(large: Boolean, onLargeChange: (Boolean) -> Unit, onLeave: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(27.dp))
        Title("我的", "按你的习惯，轻松交流。")
        Spacer(Modifier.height(25.dp))
        Surface(color = deep, shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("你好，体验者", color = white, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Spacer(Modifier.height(8.dp))
                Text("正在使用本地界面演示", color = Color(0xFFC1E9E0), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(29.dp))
        Text("阅读设置", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        Surface(color = white, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("大字字幕", color = ink, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("在字幕与记录页面使用更大的文字；选择会保存在本机", color = secondary,
                        fontSize = 12.sp, lineHeight = 18.sp)
                }
                Switch(checked = large, onCheckedChange = onLargeChange)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("关于演示", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        Surface(color = white, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(19.dp)) {
                Text("听见 Tingjian", color = ink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(9.dp))
                Text("可使用文字回复和单句语音识别；记录仍是示例内容。登录、连续字幕和云端同步尚未接入。",
                    color = secondary, fontSize = 13.sp, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(21.dp))
        TextButton(onClick = onLeave) { Text("返回欢迎页", color = teal) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = white, shadowElevation = 7.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp, bottom = 7.dp)) {
            listOf("首页", "字幕", "记录", "我的").forEachIndexed { index, name ->
                Column(Modifier.weight(1f).clickable { onSelect(index) }
                    .padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(listOf("⌂", "≋", "▤", "○")[index], fontSize = 23.sp,
                        color = if (index == selected) teal else secondary)
                    Spacer(Modifier.height(2.dp))
                    Text(name, fontSize = 11.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selected) teal else secondary)
                }
            }
        }
    }
}
