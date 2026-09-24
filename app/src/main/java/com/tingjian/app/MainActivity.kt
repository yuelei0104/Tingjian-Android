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
import com.tingjian.app.network.NetworkModule
import com.tingjian.app.network.GlossaryUpsertRequest
import com.tingjian.app.network.KeywordUpsertRequest
import com.tingjian.app.network.QuickPhraseUpsertRequest
import com.tingjian.app.data.ApiResult
import com.tingjian.app.data.toDashboard
import com.tingjian.app.data.toConversation
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 未登录或同步失败时保存在设备内；登录后同步文字会话，语音仍由设备服务处理。
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModule.initialize(applicationContext)
        enableEdgeToEdge()
        setContent { TingjianTheme(dynamicColor = false) { TingjianApp() } }
    }
}

@Composable
private fun TingjianApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { NetworkModule.repository }
    val preferences = remember { context.getSharedPreferences("tingjian_display", android.content.Context.MODE_PRIVATE) }
    var entered by remember { mutableStateOf(preferences.getBoolean("welcome_completed", false)) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Conversation?>(null) }
    var showLogin by remember { mutableStateOf(false) }
    var showUsage by remember { mutableStateOf(false) }
    var demoLoggedIn by remember { mutableStateOf(repository.isLoggedIn()) }
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
    val remoteRecords = remember { mutableStateListOf<Conversation>() }
    val glossaryTerms = remember { mutableStateListOf<GlossaryTerm>().also {
        it.addAll(loadTerms(preferences))
    } }
    val quickPhrases = remember { mutableStateListOf<QuickPhrase>().also {
        it.addAll(loadQuickPhrases(preferences))
    } }
    val keywordRules = remember { mutableStateListOf<KeywordRule>() }
    val liveLines = remember { mutableStateListOf<ChatLine>() }
    var sessionStartedAt by remember { mutableLongStateOf(0L) }
    var scene by remember { mutableStateOf("日常") }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var remoteSessionCreating by remember { mutableStateOf(false) }
    var syncNotice by remember { mutableStateOf("") }
    var personalizationVersion by remember { mutableIntStateOf(0) }
    var knownGlossaryIds by remember { mutableStateOf(emptySet<String>()) }
    var knownQuickPhraseIds by remember { mutableStateOf(emptySet<String>()) }
    var homeDashboard by remember { mutableStateOf<HomeDashboard?>(null) }
    var homeRefreshing by remember { mutableStateOf(false) }
    var homeError by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val remoteIds = remoteRecords.mapNotNull { it.serverId }.toSet()
    val visibleSavedRecords = if (demoLoggedIn) {
        remoteRecords.toList() + savedRecords.filter { it.serverId !in remoteIds }
    } else {
        savedRecords.toList()
    }
    val allRecords = visibleSavedRecords + examples
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    suspend fun reloadRemoteHistory() {
        when (val result = repository.history(page = 0, size = 50)) {
            is ApiResult.Success -> {
                remoteRecords.clear()
                remoteRecords.addAll(result.value.items.map { it.toConversation() })
                syncNotice = ""
            }
            is ApiResult.Error -> syncNotice = result.message
        }
    }

    suspend fun reloadHome(showNotice: Boolean = false) {
        if (!demoLoggedIn) return
        homeRefreshing = true
        when (val result = repository.home()) {
            is ApiResult.Success -> {
                homeDashboard = result.value.toDashboard()
                homeError = ""
            }
            is ApiResult.Error -> {
                homeError = result.message
                if (showNotice) syncNotice = result.message
            }
        }
        homeRefreshing = false
    }

    suspend fun reloadPersonalization() {
        val glossaryResult = repository.glossary()
        val phraseResult = repository.quickPhrases()
        val keywordResult = repository.keywords()
        if (glossaryResult is ApiResult.Success) {
            val remoteTerms = glossaryResult.value
            knownGlossaryIds = remoteTerms.map { it.id }.toSet()
            val hasPreviouslySyncedTerms = glossaryTerms.any { it.serverId != null }
            if (remoteTerms.isNotEmpty() || glossaryTerms.isEmpty() || hasPreviouslySyncedTerms) {
                glossaryTerms.clear()
                glossaryTerms.addAll(remoteTerms.map {
                    GlossaryTerm(
                        name = it.term,
                        alias = it.alias.orEmpty(),
                        language = it.language,
                        category = it.category,
                        priority = when {
                            it.priority >= 70 -> "高"
                            it.priority <= 30 -> "低"
                            else -> "中"
                        },
                        enabled = it.enabled,
                        serverId = it.id
                    )
                })
                saveTerms(preferences, glossaryTerms)
            }
        }
        if (phraseResult is ApiResult.Success) {
            val remotePhrases = phraseResult.value
            knownQuickPhraseIds = remotePhrases.map { it.id }.toSet()
            val hasPreviouslySyncedPhrases = quickPhrases.any { it.serverId != null }
            if (remotePhrases.isNotEmpty() || quickPhrases.isEmpty() || hasPreviouslySyncedPhrases) {
                quickPhrases.clear()
                quickPhrases.addAll(remotePhrases.sortedBy { it.sortOrder }.map {
                    QuickPhrase(it.content, it.category, it.enabled, it.id)
                })
                saveQuickPhrases(preferences, quickPhrases)
            }
        }
        if (keywordResult is ApiResult.Success) {
            keywordRules.clear()
            keywordRules.addAll(keywordResult.value.map {
                KeywordRule(it.id, it.phrase, it.vibrationEnabled, it.priority, it.enabled)
            })
        }
    }

    suspend fun syncPersonalization() {
        val termSnapshot = glossaryTerms.toList()
        val currentTermIds = termSnapshot.mapNotNull { it.serverId }.toSet()
        for (deletedId in knownGlossaryIds - currentTermIds) {
            if (repository.deleteGlossary(deletedId) is ApiResult.Error) {
                syncNotice = "术语删除同步失败"
                return
            }
        }
        for (term in termSnapshot) {
            val request = GlossaryUpsertRequest(
                term.name,
                term.alias.ifBlank { null },
                term.language,
                term.category,
                when (term.priority) { "高" -> 80; "低" -> 20; else -> 50 },
                term.enabled
            )
            val result = if (term.serverId == null) {
                repository.createGlossary(request)
            } else {
                repository.updateGlossary(term.serverId, request)
            }
            if (result is ApiResult.Error) {
                syncNotice = result.message
                return
            }
        }

        val phraseSnapshot = quickPhrases.toList()
        val currentPhraseIds = phraseSnapshot.mapNotNull { it.serverId }.toSet()
        for (deletedId in knownQuickPhraseIds - currentPhraseIds) {
            if (repository.deleteQuickPhrase(deletedId) is ApiResult.Error) {
                syncNotice = "常用语删除同步失败"
                return
            }
        }
        for ((index, phrase) in phraseSnapshot.withIndex()) {
            val request = QuickPhraseUpsertRequest(
                phrase.text, phrase.category, index, phrase.enabled
            )
            val result = if (phrase.serverId == null) {
                repository.createQuickPhrase(request)
            } else {
                repository.updateQuickPhrase(phrase.serverId, request)
            }
            if (result is ApiResult.Error) {
                syncNotice = result.message
                return
            }
        }

        val remoteKeywords = when (val result = repository.keywords()) {
            is ApiResult.Success -> result.value
            is ApiResult.Error -> {
                syncNotice = result.message
                return
            }
        }
        val desiredPhrases = termSnapshot.associateBy { it.name.lowercase(Locale.ROOT) }
        for (keyword in remoteKeywords) {
            if (keyword.phrase.lowercase(Locale.ROOT) !in desiredPhrases) {
                if (repository.deleteKeyword(keyword.id) is ApiResult.Error) {
                    syncNotice = "关键词删除同步失败"
                    return
                }
            }
        }
        for (term in termSnapshot) {
            val existing = remoteKeywords.firstOrNull {
                it.phrase.equals(term.name, ignoreCase = true)
            }
            val request = KeywordUpsertRequest(
                term.name,
                keywordVibration,
                when (term.priority) { "高" -> 80; "低" -> 20; else -> 50 },
                term.enabled
            )
            val result = if (existing == null) repository.createKeyword(request)
            else repository.updateKeyword(existing.id, request)
            if (result is ApiResult.Error) {
                syncNotice = result.message
                return
            }
        }
        reloadPersonalization()
        syncNotice = "个性化设置已同步"
    }

    fun createRemoteSession(title: String) {
        if (!demoLoggedIn || activeSessionId != null || remoteSessionCreating) return
        remoteSessionCreating = true
        scope.launch {
            when (val result = repository.createSession(title)) {
                is ApiResult.Success -> {
                    activeSessionId = result.value.id
                    syncNotice = ""
                }
                is ApiResult.Error -> syncNotice = "服务端会话创建失败，本次内容仍会保存在本机"
            }
            remoteSessionCreating = false
        }
    }

    fun openConversation(record: Conversation) {
        selected = record
        val serverId = record.serverId ?: return
        if (!demoLoggedIn || record.transcript.isNotEmpty()) return
        scope.launch {
            when (val result = repository.historyDetail(serverId)) {
                is ApiResult.Success -> selected = result.value.toConversation()
                is ApiResult.Error -> syncNotice = result.message
            }
        }
    }

    LaunchedEffect(demoLoggedIn) {
        if (demoLoggedIn) {
            reloadHome()
            reloadRemoteHistory()
            reloadPersonalization()
            if (glossaryTerms.any { it.serverId == null } ||
                quickPhrases.any { it.serverId == null }) {
                syncPersonalization()
            }
        } else {
            remoteRecords.clear()
            keywordRules.clear()
            homeDashboard = null
            homeError = ""
            homeRefreshing = false
            knownGlossaryIds = emptySet()
            knownQuickPhraseIds = emptySet()
        }
    }
    LaunchedEffect(personalizationVersion) {
        if (personalizationVersion > 0 && demoLoggedIn) {
            delay(300)
            syncPersonalization()
        }
    }
    LaunchedEffect(syncNotice) {
        if (syncNotice.isNotBlank()) {
            snackbarHostState.showSnackbar(syncNotice)
            syncNotice = ""
        }
    }

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
    Scaffold(containerColor = canvas, snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = {
        if (!showLogin && !showUsage && selected == null && !keyboardVisible) BottomTabs(tab) {
            if (it == 1 && sessionStartedAt == 0L) {
                sessionStartedAt = System.currentTimeMillis()
                createRemoteSession(if (scene == "日常") "面对面会话" else "$scene · 会话")
            }
            tab = it
        }
    }) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            val record = selected
            if (showUsage) {
                UsageScreen(savedCount = savedRecords.size, dashboard = homeDashboard,
                    onBack = { showUsage = false })
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
                    record.serverId?.let { serverId ->
                        scope.launch {
                            when (val result = repository.deleteHistory(serverId)) {
                                is ApiResult.Success -> {
                                    remoteRecords.removeAll { it.serverId == serverId }
                                    reloadHome()
                                }
                                is ApiResult.Error -> syncNotice = result.message
                            }
                        }
                    }
                    savedRecords.removeAll { it.id == record.id }
                    saveConversations(preferences, savedRecords)
                    selected = null
                })
            } else when (tab) {
                0 -> HomeScreen(allRecords, homeDashboard, homeRefreshing, homeError,
                    liveLines.isNotEmpty(), demoLoggedIn, onNew = {
                    if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
                    createRemoteSession("面对面会话")
                    tab = 1
                }, onScene = { chosen ->
                    if (liveLines.isEmpty()) scene = chosen
                    if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
                    createRemoteSession(if (chosen == "日常") "面对面会话" else "$chosen · 会话")
                    tab = 1
                }, onHistory = { tab = 2 }, onUsage = { showUsage = true },
                    onLogin = { showLogin = true },
                    onRefresh = {
                        if (demoLoggedIn) scope.launch {
                            reloadHome(showNotice = true)
                            reloadRemoteHistory()
                        }
                    },
                    onOpen = { openConversation(it) })
                1 -> LiveScreen(large, liveLines, recognitionLanguage, onLanguageChange = {
                    recognitionLanguage = it
                    preferences.edit().putString("recognition_language", it).apply()
                }, voiceMode = voiceMode, voiceStyle = voiceStyle, ttsSpeed = ttsSpeed,
                    keywords = (enabledKeywords(glossaryTerms) + keywordRules
                        .filter { it.enabled }.map { it.phrase }).distinct(),
                    keywordVibration = keywordVibration,
                    keywordHighlight = keywordHighlight,
                    quickPhrases = quickPhrases.filter { it.enabled },
                    sessionStartedAt = sessionStartedAt, onFinish = {
                    val now = System.currentTimeMillis()
                    val completedLines = liveLines.toList()
                    val completedSessionId = activeSessionId
                    val completedTitle = if (scene == "日常") "面对面会话" else "$scene · 会话"
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
                            }, id = now, scene = scene, serverId = completedSessionId
                        )
                        savedRecords.add(0, record)
                        if (savedRecords.size > 100) savedRecords.removeAt(savedRecords.lastIndex)
                        saveConversations(preferences, savedRecords)
                        selected = record
                    }
                    if (demoLoggedIn) {
                        scope.launch {
                            while (remoteSessionCreating) delay(50)
                            var serverId = completedSessionId ?: activeSessionId
                            if (serverId == null && completedLines.isNotEmpty()) {
                                when (val created = repository.createSession(completedTitle)) {
                                    is ApiResult.Success -> serverId = created.value.id
                                    is ApiResult.Error -> syncNotice = created.message
                                }
                            }
                            if (serverId != null) {
                                val actualServerId = serverId
                                val localIndex = savedRecords.indexOfFirst { it.id == now }
                                if (completedLines.isNotEmpty() && localIndex >= 0) {
                                    savedRecords[localIndex] = savedRecords[localIndex].copy(
                                        serverId = actualServerId
                                    )
                                    saveConversations(preferences, savedRecords)
                                }
                                var uploadSucceeded = true
                                for (line in completedLines) {
                                    val result = repository.addMessage(
                                        actualServerId,
                                        if (line.fromMe) "SELF" else "OTHER",
                                        line.content
                                    )
                                    if (result is ApiResult.Error) {
                                        uploadSucceeded = false
                                        syncNotice = result.message
                                        break
                                    }
                                }
                                if (uploadSucceeded) {
                                    repository.endSession(actualServerId)
                                    reloadRemoteHistory()
                                    reloadHome()
                                }
                            }
                            activeSessionId = null
                            remoteSessionCreating = false
                        }
                    }
                    liveLines.clear()
                    sessionStartedAt = 0L
                    if (!demoLoggedIn || completedLines.isEmpty()) {
                        activeSessionId = null
                        remoteSessionCreating = false
                    }
                    scene = "日常"
                })
                2 -> HistoryScreen(allRecords, onOpen = { openConversation(it) })
                else -> ProfileScreen(large, savedRecords.size, voiceMode, voiceStyle,
                    demoLoggedIn = demoLoggedIn, onLogin = { showLogin = true },
                    onLogout = {
                        scope.launch { repository.logout() }
                        demoLoggedIn = false
                    },
                    onDeleteAccount = {
                        scope.launch {
                            repository.clearAllData()
                            repository.logout()
                        }
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
                        if (demoLoggedIn) {
                            scope.launch {
                                when (val result = repository.clearHistory()) {
                                    is ApiResult.Success -> remoteRecords.clear()
                                    is ApiResult.Error -> syncNotice = result.message
                                }
                                reloadHome()
                            }
                        }
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
                    onTermsChanged = {
                        saveTerms(preferences, glossaryTerms)
                        personalizationVersion++
                    },
                    onQuickPhrasesChanged = {
                        saveQuickPhrases(preferences, quickPhrases)
                        personalizationVersion++
                    },
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
