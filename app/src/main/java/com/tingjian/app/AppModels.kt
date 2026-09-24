package com.tingjian.app

import android.Manifest
import android.content.SharedPreferences
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
internal data class Conversation(
    val title: String, val time: String, val preview: String, val duration: String,
    val transcript: List<Pair<String, String>>, val id: Long = 0L, val isExample: Boolean = false,
    val scene: String = "日常", val serverId: String? = null
)

internal data class ChatLine(val content: String, val fromMe: Boolean)

internal data class GlossaryTerm(
    val name: String,
    val alias: String = "",
    val language: String = "自动",
    val category: String = "通用",
    val priority: String = "中",
    val enabled: Boolean = true,
    val serverId: String? = null
)

internal data class QuickPhrase(
    val text: String,
    val category: String = "日常",
    val enabled: Boolean = true,
    val serverId: String? = null
)

internal data class KeywordRule(
    val id: String,
    val phrase: String,
    val vibrationEnabled: Boolean,
    val priority: Int,
    val enabled: Boolean
)

internal val examples = listOf(
    Conversation("和朋友聊聊周末", "今天 14:32", "我们周六去公园走走，怎么样？", "12 分钟",
        listOf("对方" to "我们周六去公园走走，怎么样？", "我" to "好呀，下午见！"),
        isExample = true, scene = "日常"),
    Conversation("课堂笔记 · 人工智能", "昨天 09:15", "老师说，模型的训练需要关注数据质量。", "48 分钟",
        listOf("老师" to "模型的训练需要关注数据质量。", "老师" to "下周请提交课程报告。"),
        isExample = true, scene = "课堂"),
    Conversation("家庭聚餐", "9 月 17 日", "下次见面一起做你最喜欢的菜。", "26 分钟",
        listOf("家人" to "下次见面一起做你最喜欢的菜。", "我" to "好，我很期待。"),
        isExample = true, scene = "日常")
)

internal fun loadConversations(preferences: SharedPreferences): List<Conversation> = runCatching {
    val data = JSONArray(preferences.getString("saved_conversations", "[]"))
    (0 until data.length()).mapNotNull record@{ index ->
        val item = data.optJSONObject(index) ?: return@record null
        val messages = item.optJSONArray("messages") ?: return@record null
        val transcript = (0 until messages.length()).mapNotNull message@{ i ->
            val entry = messages.optJSONObject(i) ?: return@message null
            val content = entry.optString("text")
            if (content.isBlank()) null else entry.optString("speaker", "对方") to content
        }
        if (transcript.isEmpty()) null else Conversation(
            title = item.optString("title", "面对面会话"),
            time = item.optString("time"),
            preview = transcript.last().second,
            duration = item.optString("duration"),
            transcript = transcript,
            id = item.optLong("id"),
            scene = item.optString("scene", "日常"),
            serverId = item.optString("serverId").ifBlank { null }
        )
    }
}.getOrDefault(emptyList())

internal fun saveConversations(preferences: SharedPreferences, conversations: List<Conversation>) {
    val data = JSONArray()
    conversations.forEach { conversation ->
        val messages = JSONArray()
        conversation.transcript.forEach { (speaker, content) ->
            messages.put(JSONObject().put("speaker", speaker).put("text", content))
        }
        data.put(JSONObject().put("id", conversation.id).put("title", conversation.title)
            .put("time", conversation.time).put("duration", conversation.duration)
            .put("scene", conversation.scene)
            .put("serverId", conversation.serverId ?: "")
            .put("messages", messages))
    }
    preferences.edit().putString("saved_conversations", data.toString()).apply()
}

internal fun loadTerms(preferences: SharedPreferences): List<GlossaryTerm> = runCatching {
    val data = JSONArray(preferences.getString("saved_terms", "[]"))
    (0 until data.length()).mapNotNull term@{ index ->
        val item = data.optJSONObject(index) ?: return@term null
        val name = item.optString("name")
        if (name.isBlank()) null else GlossaryTerm(
            name = name,
            alias = item.optString("alias"),
            language = item.optString("language", "自动"),
            category = item.optString("category", "通用"),
            priority = item.optString("priority", "中"),
            enabled = item.optBoolean("enabled", true),
            serverId = item.optString("serverId").ifBlank { null }
        )
    }
}.getOrDefault(emptyList())

internal fun saveTerms(preferences: SharedPreferences, terms: List<GlossaryTerm>) {
    val data = JSONArray()
    terms.forEach { term ->
        data.put(JSONObject().put("name", term.name).put("alias", term.alias)
            .put("language", term.language).put("category", term.category)
            .put("priority", term.priority).put("enabled", term.enabled)
            .put("serverId", term.serverId ?: ""))
    }
    preferences.edit().putString("saved_terms", data.toString()).apply()
}

internal fun loadQuickPhrases(preferences: SharedPreferences): List<QuickPhrase> = runCatching {
    val raw = preferences.getString("quick_phrases", null)
    if (raw == null) return@runCatching listOf(
        QuickPhrase("请说慢一点，谢谢。"), QuickPhrase("请再说一次。"),
        QuickPhrase("好的，我明白了。"), QuickPhrase("Please repeat that.", "English")
    )
    val data = JSONArray(raw)
    (0 until data.length()).mapNotNull { index ->
        val item = data.optJSONObject(index) ?: return@mapNotNull null
        val text = item.optString("text")
        if (text.isBlank()) null else QuickPhrase(text, item.optString("category", "日常"),
            item.optBoolean("enabled", true), item.optString("serverId").ifBlank { null })
    }
}.getOrDefault(emptyList())

internal fun saveQuickPhrases(preferences: SharedPreferences, phrases: List<QuickPhrase>) {
    val data = JSONArray()
    phrases.forEach { phrase ->
        data.put(JSONObject().put("text", phrase.text).put("category", phrase.category)
            .put("enabled", phrase.enabled).put("serverId", phrase.serverId ?: ""))
    }
    preferences.edit().putString("quick_phrases", data.toString()).apply()
}

internal fun enabledKeywords(terms: List<GlossaryTerm>): List<String> = terms
    .filter { it.enabled }
    .flatMap { term -> listOf(term.name) + term.alias.split(',', '，', '/', '、') }
    .map { it.trim() }.filter { it.isNotEmpty() }.distinct()

internal fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

internal fun localSummary(record: Conversation): String {
    val messages = record.transcript.map { it.second.trim() }.filter { it.isNotEmpty() }
    if (messages.isEmpty()) return "这段会话暂时没有可供整理的文字。"
    val focus = messages.take(2).joinToString("；").take(90)
    val ending = messages.last().take(45)
    return "会话共 ${messages.size} 条文字。主要内容：$focus。" +
        if (messages.size > 2) "最后提到：$ending。" else ""
}
