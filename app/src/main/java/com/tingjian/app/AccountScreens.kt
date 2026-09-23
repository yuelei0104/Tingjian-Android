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
import kotlinx.coroutines.launch
import com.tingjian.app.data.ApiResult
import com.tingjian.app.network.NetworkModule

@Composable
internal fun UsageScreen(savedCount: Int, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(canvas).verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(14.dp))
        Title("用量与额度", "V1 内测界面预览，暂未开放购买。")
        Spacer(Modifier.height(18.dp))
        Pill("演示数值 · 不会自动扣费", highlighted = true)
        Spacer(Modifier.height(20.dp))
        UsageCard("实时识别", "剩余 45 分钟", 0.75f,
            "本期演示额度 60 分钟 · 已用 15 分钟")
        Spacer(Modifier.height(12.dp))
        UsageCard("云端播报", "剩余 8000 字符", 0.8f,
            "当前应用优先使用设备语音引擎，演示额度不会真实扣除")
        Spacer(Modifier.height(12.dp))
        UsageCard("AI 功能", "剩余 20 次", 0.67f,
            "表达建议与摘要目前仅提供本地界面预览")
        Spacer(Modifier.height(18.dp))
        Surface(color = white, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("本机数据", color = ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("已保存 $savedCount 段会话", color = secondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("以上额度均为原型演示，不代表正式套餐。真实计费、购买和服务端用量将在后端接入后确定。",
            color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
internal fun UsageCard(title: String, value: String, progress: Float, note: String) {
    Surface(color = white, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = ink, fontWeight = FontWeight.Bold)
                Text(value, color = teal, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
                color = teal, trackColor = divider)
            Spacer(Modifier.height(9.dp))
            Text(note, color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun DemoLoginScreen(onBack: () -> Unit, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember { NetworkModule.repository }
    var registerMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var legal by remember { mutableStateOf("") }
    var loginState by remember { mutableStateOf("idle") }
    if (legal.isNotEmpty()) {
        AlertDialog(onDismissRequest = { legal = "" },
            title = { Text(if (legal == "terms") "用户协议" else "隐私政策") },
            text = { Text(if (legal == "terms")
                "注册和登录仅用于保存你的服务数据。当前版本不提供付费功能。"
            else "账号密码会通过当前配置的听见后端传输；密码不会以明文保存在服务端。" +
                "麦克风只在你主动点击语音识别时使用。") },
            confirmButton = {
                TextButton(onClick = { legal = "" }) { Text("知道了", color = teal) }
            })
    }
    Column(Modifier.fillMaxSize().background(canvas).verticalScroll(rememberScrollState())
        .imePadding().padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(28.dp))
        BrandMark()
        Spacer(Modifier.height(30.dp))
        Pill(if (registerMode) "创建账户" else "账户登录", highlighted = true)
        Spacer(Modifier.height(17.dp))
        Title("欢迎使用听见", if (registerMode) "注册后即可同步你的服务数据。" else "登录后连接听见后端服务。")
        Spacer(Modifier.height(24.dp))
        Surface(color = white, shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                if (registerMode) {
                    OutlinedTextField(value = displayName, onValueChange = {
                        displayName = it.take(40)
                        error = ""
                    }, label = { Text("昵称") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(value = email, onValueChange = {
                    email = it.take(254)
                    error = ""
                }, label = { Text("邮箱") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = {
                    password = it.take(72)
                    error = ""
                }, label = { Text("密码（至少 8 位）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth())
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = secondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Column {
                        Text("我已阅读并同意相关说明", fontSize = 13.sp, color = secondary)
                        Row {
                            TextButton(onClick = { legal = "terms" },
                                contentPadding = PaddingValues(end = 8.dp)) {
                                Text("用户协议", color = teal, fontSize = 12.sp)
                            }
                            TextButton(onClick = { legal = "privacy" },
                                contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("隐私政策", color = teal, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (email.isBlank() || password.length < 8 ||
                        (registerMode && displayName.isBlank())) {
                        error = "请填写有效邮箱、至少 8 位密码${if (registerMode) "和昵称" else ""}"
                        return@Button
                    }
                    loginState = "loading"
                    error = ""
                    scope.launch {
                        val result = if (registerMode) {
                            repository.register(email, password, displayName)
                        } else {
                            repository.login(email, password)
                        }
                        when (result) {
                            is ApiResult.Success -> {
                                loginState = "success"
                                onLogin()
                            }
                            is ApiResult.Error -> {
                                loginState = "failed"
                                error = result.message
                            }
                        }
                    }
                }, enabled = consent && loginState != "loading",
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                    Text(if (loginState == "loading") "请稍候…" else if (registerMode) "注册并登录" else "登录")
                }
                TextButton(onClick = {
                    registerMode = !registerMode
                    error = ""
                }, modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = loginState != "loading") {
                    Text(if (registerMode) "已有账户？返回登录" else "没有账户？立即注册", color = teal)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("若无法连接，请确认后端已启动，并检查 TINGJIAN_API_BASE_URL 配置。",
            color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
internal fun ProfileScreen(large: Boolean, savedCount: Int, voiceMode: String,
    voiceStyle: String, demoLoggedIn: Boolean, onLogin: () -> Unit, onLogout: () -> Unit,
    onDeleteAccount: () -> Unit, onUsage: () -> Unit, onClearHistory: () -> Unit,
    onVoiceModeChange: (String) -> Unit, onVoiceStyleChange: (String) -> Unit,
    ttsSpeed: Float, onTtsSpeedChange: (Float) -> Unit,
    recognitionLanguage: String, onLanguageChange: (String) -> Unit,
    keywordVibration: Boolean, onKeywordVibrationChange: (Boolean) -> Unit,
    keywordHighlight: Boolean, onKeywordHighlightChange: (Boolean) -> Unit,
    terms: SnapshotStateList<GlossaryTerm>, quickPhrases: SnapshotStateList<QuickPhrase>,
    onTermsChanged: () -> Unit, onQuickPhrasesChanged: () -> Unit,
    autoSummary: Boolean, onAutoSummaryChange: (Boolean) -> Unit,
    onLargeChange: (Boolean) -> Unit, onLeave: () -> Unit) {
    val context = LocalContext.current
    var newTerm by remember { mutableStateOf("") }
    var newAlias by remember { mutableStateOf("") }
    var newTermLanguage by remember { mutableStateOf("自动") }
    var newTermCategory by remember { mutableStateOf("通用") }
    var newTermPriority by remember { mutableStateOf("中") }
    var termError by remember { mutableStateOf("") }
    var editingTermIndex by remember { mutableIntStateOf(-1) }
    var newPhrase by remember { mutableStateOf("") }
    var newPhraseCategory by remember { mutableStateOf("日常") }
    var editingPhraseIndex by remember { mutableIntStateOf(-1) }
    var deleteCode by remember { mutableStateOf("") }
    var deleteAgreed by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    if (dialog.isNotEmpty()) {
        AlertDialog(onDismissRequest = { dialog = "" }, title = {
            Text(when (dialog) {
                "voice" -> "播报语言"
                "language" -> "默认识别语言"
                "usage" -> "本机使用情况"
                "service" -> "识别服务状态"
                "terms" -> "我的术语与热词"
                "phrases" -> "快捷短语管理"
                "errors" -> "异常状态预览"
                "logout" -> "退出登录？"
                "delete" -> "清除账户数据"
                "clear" -> "清空全部会话？"
                else -> "隐私与数据"
            })
        }, text = {
            when (dialog) {
                "voice" -> Column {
                    Text("发送文字后由设备语音引擎朗读；不同设备的实际音色可能不同。",
                        fontSize = 13.sp, color = secondary)
                    Text("播报语言", color = ink, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp))
                    listOf("自动", "中文", "English").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable {
                            onVoiceModeChange(option)
                        }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = voiceMode == option, onClick = {
                                onVoiceModeChange(option)
                            })
                            Text(option, color = ink)
                        }
                    }
                    Text("音色风格", color = ink, fontWeight = FontWeight.Bold)
                    listOf("自然", "清晰", "舒缓").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { onVoiceStyleChange(option) }
                            .padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = voiceStyle == option,
                                onClick = { onVoiceStyleChange(option) })
                            Text(option, color = ink)
                        }
                    }
                    Text("播报语速", color = ink, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp))
                    listOf(0.8f to "慢速 0.8x", 1.0f to "标准 1.0x", 1.2f to "快速 1.2x")
                        .forEach { (speed, label) ->
                            Row(Modifier.fillMaxWidth().clickable { onTtsSpeedChange(speed) }
                                .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = ttsSpeed == speed,
                                    onClick = { onTtsSpeedChange(speed) })
                                Text(label, color = ink)
                            }
                        }
                }
                "language" -> Column {
                    Text("识别语言会在下一次开始识别时生效。",
                        fontSize = 13.sp, color = secondary)
                    listOf("中英混合", "中文", "English").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable {
                            onLanguageChange(option); dialog = ""
                        }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = recognitionLanguage == option, onClick = {
                                onLanguageChange(option); dialog = ""
                            })
                            Text(option, color = ink)
                        }
                    }
                }
                "terms" -> Column(Modifier.heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text("词条用于本机关键词高亮和震动；云端热词增强将在识别服务接入后启用。",
                        color = secondary, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newTerm, onValueChange = { newTerm = it.take(30) },
                        label = { Text("术语") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(value = newAlias, onValueChange = { newAlias = it.take(30) },
                        label = { Text("别名或读法（可选）") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Text("语言", color = secondary, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("自动", "中文", "English").forEach { option ->
                            FilterChip(selected = newTermLanguage == option,
                                onClick = { newTermLanguage = option }, label = { Text(option) })
                        }
                    }
                    Text("类别与优先级", color = secondary, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("通用", "人名", "课程", "技术").forEach { option ->
                            FilterChip(selected = newTermCategory == option,
                                onClick = { newTermCategory = option }, label = { Text(option) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("高", "中", "低").forEach { option ->
                            FilterChip(selected = newTermPriority == option,
                                onClick = { newTermPriority = option }, label = { Text("${option}优先级") })
                        }
                    }
                    TextButton(onClick = {
                        val name = newTerm.trim()
                        if (terms.withIndex().any {
                            it.index != editingTermIndex &&
                                it.value.name.equals(name, ignoreCase = true)
                        }) {
                            termError = "该词条已经存在"
                        } else if (name.length > 30) {
                            termError = "词条不能超过 30 个字符"
                        } else if (name.isNotEmpty()) {
                            val updated = GlossaryTerm(name, newAlias.trim(), newTermLanguage,
                                newTermCategory, newTermPriority,
                                terms.getOrNull(editingTermIndex)?.enabled ?: true)
                            if (editingTermIndex in terms.indices) terms[editingTermIndex] = updated
                            else terms.add(updated)
                            onTermsChanged()
                            newTerm = ""
                            newAlias = ""
                            editingTermIndex = -1
                            termError = ""
                        }
                    }, enabled = newTerm.isNotBlank()) {
                        Text(if (editingTermIndex >= 0) "保存修改" else "添加术语", color = teal)
                    }
                    if (termError.isNotEmpty()) Text(termError,
                        color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    terms.forEachIndexed { index, term ->
                        Surface(color = canvas, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (term.alias.isBlank()) term.name
                                            else "${term.name} · ${term.alias}",
                                            color = ink, fontSize = 14.sp)
                                        Text("${term.language} · ${term.category} · ${term.priority}优先级",
                                            color = secondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = term.enabled, onCheckedChange = {
                                        terms[index] = term.copy(enabled = it)
                                        onTermsChanged()
                                    })
                                }
                                Row {
                                    TextButton(onClick = {
                                        editingTermIndex = index
                                        newTerm = term.name
                                        newAlias = term.alias
                                        newTermLanguage = term.language
                                        newTermCategory = term.category
                                        newTermPriority = term.priority
                                    }) { Text("编辑") }
                                    TextButton(onClick = {
                                        terms.removeAt(index)
                                        onTermsChanged()
                                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
                "phrases" -> Column(Modifier.heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text("快捷短语保存在本机，可启用、删除并用上下按钮调整顺序。",
                        color = secondary, fontSize = 13.sp, lineHeight = 19.sp)
                    OutlinedTextField(value = newPhrase,
                        onValueChange = { newPhrase = it.take(80) },
                        label = { Text("新增快捷短语") }, modifier = Modifier.fillMaxWidth())
                    listOf("日常", "课堂", "会议", "就医", "English").chunked(3).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            group.forEach { option ->
                                FilterChip(selected = newPhraseCategory == option,
                                    onClick = { newPhraseCategory = option },
                                    label = { Text(option) })
                            }
                        }
                    }
                    TextButton(onClick = {
                        val updated = QuickPhrase(newPhrase.trim(), newPhraseCategory,
                            quickPhrases.getOrNull(editingPhraseIndex)?.enabled ?: true)
                        if (editingPhraseIndex in quickPhrases.indices) {
                            quickPhrases[editingPhraseIndex] = updated
                        } else quickPhrases.add(updated)
                        newPhrase = ""
                        editingPhraseIndex = -1
                        onQuickPhrasesChanged()
                    }, enabled = newPhrase.isNotBlank() &&
                        quickPhrases.withIndex().none {
                            it.index != editingPhraseIndex && it.value.text == newPhrase.trim()
                        }) {
                        Text(if (editingPhraseIndex >= 0) "保存修改" else "添加短语", color = teal)
                    }
                    quickPhrases.forEachIndexed { index, phrase ->
                        Surface(color = canvas, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(phrase.text, color = ink, fontSize = 14.sp)
                                        Text(phrase.category, color = secondary, fontSize = 11.sp)
                                    }
                                    Switch(checked = phrase.enabled, onCheckedChange = {
                                        quickPhrases[index] = phrase.copy(enabled = it)
                                        onQuickPhrasesChanged()
                                    })
                                }
                                Row {
                                    TextButton(onClick = {
                                        if (index > 0) {
                                            quickPhrases.removeAt(index)
                                            quickPhrases.add(index - 1, phrase)
                                            onQuickPhrasesChanged()
                                        }
                                    }, enabled = index > 0) { Text("上移") }
                                    TextButton(onClick = {
                                        if (index < quickPhrases.lastIndex) {
                                            quickPhrases.removeAt(index)
                                            quickPhrases.add(index + 1, phrase)
                                            onQuickPhrasesChanged()
                                        }
                                    }, enabled = index < quickPhrases.lastIndex) { Text("下移") }
                                    TextButton(onClick = {
                                        editingPhraseIndex = index
                                        newPhrase = phrase.text
                                        newPhraseCategory = phrase.category
                                    }) { Text("编辑") }
                                    TextButton(onClick = {
                                        quickPhrases.removeAt(index)
                                        onQuickPhrasesChanged()
                                    }) { Text("删除") }
                                }
                            }
                        }
                    }
                }
                "errors" -> Column(Modifier.heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text("用于检查各种异常提示的前端样式，不会改变真实网络或权限。",
                        color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
                    listOf(
                        "网络不可用" to "云端识别已停止；已有字幕保留，仍可输入文字。",
                        "麦克风权限被拒绝" to "请在系统设置中授权后重试。",
                        "识别失败" to "没有识别到内容，请重试。",
                        "识别额度耗尽" to "不会自动扣费，可继续使用文字沟通。",
                        "AI 额度耗尽" to "原文保留，可直接使用原文。",
                        "播报失败" to "保留原文，可重试或直接展示给对方。",
                        "登录已过期" to "返回登录页；本机未上传的会话仍保留。",
                        "语言模式不支持" to "保留已有字幕，可切换为仅中文或仅英文。",
                        "操作过于频繁" to "显示等待时间，不自动高频重试。",
                        "来电或应用进入后台" to "自动暂停录音，返回后由用户确认继续。",
                        "保存失败" to "保留当前文字并提供重新保存或导出入口。"
                    ).forEach { (title, message) ->
                        Surface(color = canvas, shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(title, color = ink, fontWeight = FontWeight.SemiBold)
                                Text(message, color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
                "logout" -> Column {
                    Text("退出不会删除本机保存的会话和设置。未发送的输入草稿仍保留在字幕页。",
                        color = ink, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onLogout(); dialog = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = teal),
                        modifier = Modifier.fillMaxWidth()) { Text("确认退出") }
                }
                "delete" -> Column {
                    Text("这会清除服务器上的会话与个性化数据，同时清除本机会话；该操作不可撤销。",
                        color = ink, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = deleteCode, onValueChange = {
                        deleteCode = it.filter(Char::isDigit).take(6)
                    }, label = { Text("输入确认码 123456") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = deleteAgreed, onCheckedChange = { deleteAgreed = it })
                        Text("我了解本机会话也会清除", fontSize = 13.sp, color = secondary)
                    }
                    Button(onClick = {
                        terms.clear()
                        quickPhrases.clear()
                        onTermsChanged()
                        onQuickPhrasesChanged()
                        onDeleteAccount()
                        deleteCode = ""
                        deleteAgreed = false
                        dialog = ""
                    }, enabled = deleteCode == "123456" && deleteAgreed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()) { Text("验证并清除") }
                }
                "clear" -> Column {
                    Text("将删除这台设备上保存的全部真实会话。示例内容会继续保留，删除后无法恢复。",
                        color = ink, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onClearHistory(); dialog = "" },
                        enabled = savedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()) { Text("确认清空全部会话") }
                }
                "usage" -> Text("已保存 $savedCount 段本机会话。语音识别和播报调用设备服务，" +
                    "应用无法统计服务商的用量或额度。当前版本没有付费功能。",
                    fontSize = 14.sp, color = ink, lineHeight = 22.sp)
                "service" -> Text(
                    if (SpeechRecognizer.isRecognitionAvailable(context))
                        "检测到系统语音识别服务。点击字幕页中的“语音”，授权麦克风后可尝试识别。" +
                            "具体网络需求由设备语音服务决定。"
                    else "当前设备没有可用的语音识别服务。你仍可输入文字并使用设备语音引擎播报。",
                    fontSize = 14.sp, color = ink, lineHeight = 22.sp)
                else -> Text("原始录音默认不保存。麦克风权限仅在点击“语音”时请求；系统识别服务" +
                    "可能根据其实现上传音频，具体是否联网取决于设备服务商。应用只保存识别后的文字，" +
                    "关闭自动备份，不进行账户同步；可删除单条或清空全部会话。课堂、会议等场景可能" +
                    "涉及他人声音，请遵守所在地录音和隐私规则。",
                    fontSize = 14.sp, color = ink, lineHeight = 22.sp)
            }
        }, confirmButton = {
            TextButton(onClick = { dialog = "" }) { Text("关闭", color = teal) }
        })
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(27.dp))
        Title("我的", "按你的习惯，轻松交流。")
        Spacer(Modifier.height(25.dp))
        Surface(color = deep, shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text(if (demoLoggedIn) "听见 · 已登录" else "听见 · 本机体验", color = white,
                    fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Spacer(Modifier.height(8.dp))
                Text("已保存 $savedCount 段本机会话 · 不上传、不跨设备同步",
                    color = Color(0xFFC1E9E0), fontSize = 13.sp)
                Spacer(Modifier.height(13.dp))
                TextButton(onClick = {
                    if (demoLoggedIn) dialog = "logout" else onLogin()
                },
                    contentPadding = PaddingValues(0.dp)) {
                    Text(if (demoLoggedIn) "退出登录" else "登录或注册  →",
                        color = Color(0xFFDDF8EF), fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("交流设置", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Surface(color = white, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("大字字幕", color = ink, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("字幕和记录使用更大的文字", color = secondary, fontSize = 12.sp)
                }
                Switch(checked = large, onCheckedChange = onLargeChange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = white, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("关键词震动", color = ink, fontWeight = FontWeight.SemiBold)
                    Text("本机命中词条后短震动；同一词条 10 秒内不重复提醒",
                        color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Switch(checked = keywordVibration, onCheckedChange = onKeywordVibrationChange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = white, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("关键词高亮", color = ink, fontWeight = FontWeight.SemiBold)
                    Text("在实时字幕和会话详情中突出显示命中词",
                        color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Switch(checked = keywordHighlight, onCheckedChange = onKeywordHighlightChange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(color = white, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("会话自动摘要", color = ink, fontWeight = FontWeight.SemiBold)
                    Text("默认关闭；AI 服务接入后结束会话时生成",
                        color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Switch(checked = autoSummary, onCheckedChange = onAutoSummaryChange)
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsItem("默认识别语言", recognitionLanguage) { dialog = "language" }
        Spacer(Modifier.height(10.dp))
        SettingsItem("播报语言、音色与语速",
            "$voiceMode · $voiceStyle · ${ttsSpeed}x") { dialog = "voice" }
        Spacer(Modifier.height(10.dp))
        SettingsItem("我的术语与热词", "${terms.size} 个本机词条") { dialog = "terms" }
        Spacer(Modifier.height(10.dp))
        SettingsItem("快捷短语管理", "${quickPhrases.count { it.enabled }} 条已启用") {
            dialog = "phrases"
        }
        Spacer(Modifier.height(27.dp))
        Text("数据与说明", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        SettingsItem("用量与额度", "演示额度 · 暂未开放购买") { onUsage() }
        Spacer(Modifier.height(10.dp))
        SettingsItem("隐私与数据", "麦克风权限 · 本机存储") { dialog = "privacy" }
        Spacer(Modifier.height(10.dp))
        SettingsItem("清空全部会话", if (savedCount > 0) "将删除 $savedCount 段本机会话"
            else "当前没有本机会话") { dialog = "clear" }
        Spacer(Modifier.height(10.dp))
        SettingsItem("识别服务状态",
            if (SpeechRecognizer.isRecognitionAvailable(context)) "当前设备发现语音识别服务"
            else "当前设备未发现语音识别服务，仍可使用文字播报") {
            dialog = "service"
        }
        Spacer(Modifier.height(10.dp))
        SettingsItem("异常状态预览", "断网 · 权限 · 额度 · 播报失败") { dialog = "errors" }
        if (demoLoggedIn) {
            Spacer(Modifier.height(10.dp))
            SettingsItem("清除账户数据", "清除服务端数据和本机会话") {
                deleteCode = ""
                deleteAgreed = false
                dialog = "delete"
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("AI 表达助手、云端同步及用量套餐将在后续版本开放。",
            color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onLeave) { Text("返回欢迎页", color = teal) }
        Spacer(Modifier.height(24.dp))
    }
}
