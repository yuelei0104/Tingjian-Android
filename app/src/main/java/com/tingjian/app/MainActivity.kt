package com.tingjian.app

import android.Manifest
import android.content.SharedPreferences
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

// 真实会话只保存在设备内；示例记录单独标记。语音由设备识别和播报服务处理。
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
    var showLogin by remember { mutableStateOf(false) }
    var showUsage by remember { mutableStateOf(false) }
    var demoLoggedIn by remember { mutableStateOf(false) }
    var large by remember { mutableStateOf(preferences.getBoolean("large_text", false)) }
    var voiceMode by remember { mutableStateOf(preferences.getString("voice_mode", "自动") ?: "自动") }
    var voiceStyle by remember { mutableStateOf(preferences.getString("voice_style", "自然") ?: "自然") }
    var keywordVibration by remember {
        mutableStateOf(preferences.getBoolean("keyword_vibration", true))
    }
    var keywordHighlight by remember {
        mutableStateOf(preferences.getBoolean("keyword_highlight", true))
    }
    var ttsSpeed by remember { mutableFloatStateOf(preferences.getFloat("tts_speed", 1.0f)) }
    var autoSummary by remember { mutableStateOf(preferences.getBoolean("auto_summary", false)) }
    var recognitionLanguage by remember {
        mutableStateOf(preferences.getString("recognition_language", "中英混合") ?: "中英混合")
    }
    val savedRecords = remember { mutableStateListOf<Conversation>().also {
        it.addAll(loadConversations(preferences))
    } }
    val glossaryTerms = remember { mutableStateListOf<GlossaryTerm>().also {
        it.addAll(loadTerms(preferences))
    } }
    val quickPhrases = remember { mutableStateListOf<QuickPhrase>().also {
        it.addAll(loadQuickPhrases(preferences))
    } }
    val liveLines = remember { mutableStateListOf<ChatLine>() }
    var sessionStartedAt by remember { mutableLongStateOf(0L) }
    var scene by remember { mutableStateOf("日常") }
    val allRecords = savedRecords.toList() + examples
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = entered && (showLogin || showUsage)) {
        showLogin = false
        showUsage = false
    }
    BackHandler(enabled = entered && !showLogin && !showUsage && selected != null) { selected = null }
    BackHandler(enabled = entered && !showLogin && !showUsage && selected == null && tab != 0) { tab = 0 }

    if (!entered) {
        WelcomeScreen {
            entered = true
            preferences.edit().putBoolean("welcome_completed", true).apply()
        }
        return
    }
    Scaffold(containerColor = canvas, bottomBar = {
        if (!showLogin && !showUsage && selected == null && !keyboardVisible) BottomTabs(tab) {
            if (it == 1 && sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
            tab = it
        }
    }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            val record = selected
            if (showUsage) {
                UsageScreen(savedCount = savedRecords.size, onBack = { showUsage = false })
            } else if (showLogin) {
                DemoLoginScreen(onBack = { showLogin = false }, onLogin = {
                    demoLoggedIn = true
                    showLogin = false
                    tab = 3
                })
            } else if (record != null) {
                DetailScreen(record, large, autoSummary,
                    if (keywordHighlight) enabledKeywords(glossaryTerms) else emptyList(),
                    onBack = { selected = null },
                    onRename = { newTitle ->
                    val index = savedRecords.indexOfFirst { it.id == record.id }
                    if (index >= 0) {
                        val renamed = savedRecords[index].copy(title = newTitle)
                        savedRecords[index] = renamed
                        saveConversations(preferences, savedRecords)
                        selected = renamed
                    }
                }, onDelete = {
                    savedRecords.removeAll { it.id == record.id }
                    saveConversations(preferences, savedRecords)
                    selected = null
                })
            } else when (tab) {
                0 -> HomeScreen(allRecords, liveLines.isNotEmpty(), demoLoggedIn, onNew = {
                    if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
                    tab = 1
                }, onScene = { chosen ->
                    if (liveLines.isEmpty()) scene = chosen
                    if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
                    tab = 1
                }, onHistory = { tab = 2 }, onUsage = { showUsage = true },
                    onLogin = { showLogin = true },
                    onOpen = { selected = it })
                1 -> LiveScreen(large, liveLines, recognitionLanguage, onLanguageChange = {
                    recognitionLanguage = it
                    preferences.edit().putString("recognition_language", it).apply()
                }, voiceMode = voiceMode, voiceStyle = voiceStyle, ttsSpeed = ttsSpeed,
                    keywords = enabledKeywords(glossaryTerms),
                    keywordVibration = keywordVibration,
                    keywordHighlight = keywordHighlight,
                    quickPhrases = quickPhrases.filter { it.enabled },
                    sessionStartedAt = sessionStartedAt, onFinish = {
                    val now = System.currentTimeMillis()
                    if (liveLines.isNotEmpty()) {
                        val start = if (sessionStartedAt == 0L) now else sessionStartedAt
                        val record = Conversation(
                            title = if (scene == "日常") "面对面会话" else "$scene · 会话",
                            time = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
                                .format(Date(start)),
                            preview = liveLines.last().content,
                            duration = "${(now - start).coerceAtLeast(0L) / 60000L + 1} 分钟",
                            transcript = liveLines.map { (text, fromMe) ->
                                (if (fromMe) "我" else "对方") to text
                            }, id = now, scene = scene
                        )
                        savedRecords.add(0, record)
                        if (savedRecords.size > 100) savedRecords.removeAt(savedRecords.lastIndex)
                        saveConversations(preferences, savedRecords)
                        selected = record
                    }
                    liveLines.clear()
                    sessionStartedAt = 0L
                    scene = "日常"
                })
                2 -> HistoryScreen(allRecords, onOpen = { selected = it })
                else -> ProfileScreen(large, savedRecords.size, voiceMode, voiceStyle,
                    demoLoggedIn = demoLoggedIn, onLogin = { showLogin = true },
                    onLogout = { demoLoggedIn = false },
                    onDeleteAccount = {
                        demoLoggedIn = false
                        savedRecords.clear()
                        saveConversations(preferences, savedRecords)
                        liveLines.clear()
                        sessionStartedAt = 0L
                        glossaryTerms.clear()
                        quickPhrases.clear()
                        saveTerms(preferences, glossaryTerms)
                        saveQuickPhrases(preferences, quickPhrases)
                    }, onUsage = { showUsage = true },
                    onClearHistory = {
                        savedRecords.clear()
                        saveConversations(preferences, savedRecords)
                    },
                    onVoiceModeChange = {
                        voiceMode = it
                        preferences.edit().putString("voice_mode", it).apply()
                    }, onVoiceStyleChange = {
                        voiceStyle = it
                        preferences.edit().putString("voice_style", it).apply()
                    }, ttsSpeed = ttsSpeed, onTtsSpeedChange = {
                        ttsSpeed = it
                        preferences.edit().putFloat("tts_speed", it).apply()
                    }, recognitionLanguage = recognitionLanguage,
                    onLanguageChange = {
                        recognitionLanguage = it
                        preferences.edit().putString("recognition_language", it).apply()
                    }, keywordVibration = keywordVibration,
                    onKeywordVibrationChange = {
                        keywordVibration = it
                        preferences.edit().putBoolean("keyword_vibration", it).apply()
                    }, keywordHighlight = keywordHighlight,
                    onKeywordHighlightChange = {
                        keywordHighlight = it
                        preferences.edit().putBoolean("keyword_highlight", it).apply()
                    }, terms = glossaryTerms, quickPhrases = quickPhrases,
                    onTermsChanged = { saveTerms(preferences, glossaryTerms) },
                    onQuickPhrasesChanged = { saveQuickPhrases(preferences, quickPhrases) },
                    autoSummary = autoSummary, onAutoSummaryChange = {
                        autoSummary = it
                        preferences.edit().putBoolean("auto_summary", it).apply()
                    }, onLargeChange = {
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
