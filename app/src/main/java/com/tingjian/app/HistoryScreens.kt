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

@Composable
internal fun HistoryScreen(records: List<Conversation>, onOpen: (Conversation) -> Unit) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("全部") }
    var visibleCount by remember { mutableIntStateOf(10) }
    val filtered = records.filter {
        val matchesSearch = it.title.contains(search, ignoreCase = true) ||
            it.preview.contains(search, ignoreCase = true) ||
            it.transcript.any { (_, text) -> text.contains(search, ignoreCase = true) }
        val matchesFilter = when (filter) {
            "今天" -> it.time.startsWith("今天")
            "全部" -> true
            else -> it.scene == filter || it.title.contains(filter)
        }
        matchesSearch && matchesFilter
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(27.dp))
        Title("会话记录", "重要的对话，随时回来看看。")
        Spacer(Modifier.height(23.dp))
        OutlinedTextField(value = search, onValueChange = { search = it; visibleCount = 10 },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("搜索会话内容") }, shape = RoundedCornerShape(16.dp),
            trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) {
                Text("清除", fontSize = 12.sp, color = teal)
            } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = teal, unfocusedBorderColor = divider,
                focusedContainerColor = white, unfocusedContainerColor = white))
        Spacer(Modifier.height(23.dp))
        listOf("全部", "今天", "课堂", "会议", "就医", "日常").chunked(3).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()) {
                group.forEach { option ->
                    FilterChip(selected = filter == option, onClick = {
                        filter = option
                        visibleCount = 10
                    }, label = { Text(option) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("全部记录", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${filtered.size} 条", color = secondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(15.dp))
        if (filtered.isEmpty()) {
            Surface(color = white, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, divider), modifier = Modifier.fillMaxWidth()) {
                Text("没有找到相关会话", Modifier.padding(30.dp),
                    color = secondary, textAlign = TextAlign.Center)
            }
        }
        filtered.take(visibleCount).forEach {
            ConversationCard(it) { onOpen(it) }
            Spacer(Modifier.height(11.dp))
        }
        if (filtered.size > visibleCount) {
            OutlinedButton(onClick = { visibleCount += 10 },
                modifier = Modifier.fillMaxWidth()) {
                Text("加载更多（剩余 ${filtered.size - visibleCount} 条）")
            }
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.any { it.isExample }) {
            Text("“示例”会话仅供体验；你保存的会话只存在当前设备。",
                color = secondary, fontSize = 12.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
internal fun DetailScreen(record: Conversation, large: Boolean, autoSummary: Boolean,
    keywords: List<String>,
    onBack: () -> Unit,
    onRename: (String) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember(record.id) { mutableStateOf(false) }
    var rename by remember(record.id) { mutableStateOf(false) }
    var newTitle by remember(record.id) { mutableStateOf(record.title) }
    var summary by remember(record.id) { mutableStateOf("") }
    var summaryState by remember(record.id) {
        mutableStateOf(if (autoSummary) "generating" else "empty")
    }
    LaunchedEffect(record.id, summaryState) {
        if (summaryState == "generating") {
            delay(700)
            summary = localSummary(record)
            summaryState = if (record.transcript.isEmpty()) "unavailable" else "done"
        }
    }
    if (rename) {
        AlertDialog(onDismissRequest = { rename = false },
            title = { Text("会话名称") },
            text = {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it.take(40) },
                    label = { Text("名称") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = newTitle.trim()
                    if (title.isNotEmpty()) {
                        onRename(title)
                        rename = false
                    }
                }, enabled = newTitle.isNotBlank()) { Text("保存", color = teal) }
            },
            dismissButton = {
                TextButton(onClick = { rename = false }) { Text("取消") }
            })
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条会话？") },
            text = { Text("删除后无法找回本机保存的这段文字。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            })
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(16.dp))
        Title(record.title, "${record.time}  ·  ${record.duration}" +
            if (record.isExample) "  ·  示例会话" else "")
        Spacer(Modifier.height(29.dp))
        Text("会话原文", color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        record.transcript.forEachIndexed { index, (speaker, content) ->
            ChatBubble(content, fromMe = speaker == "我", large = large,
                speaker = "$speaker · 第 ${index + 1} 条", keywords = keywords)
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        Surface(color = mint, shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(17.dp)) {
                Text("会话摘要", color = ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(when (summaryState) {
                    "generating" -> "正在生成摘要…"
                    "failed" -> "摘要生成失败，原始转写不受影响。"
                    "unavailable" -> "暂无可用摘要。"
                    "done" -> summary
                    else -> "还没有摘要。点击下方按钮可预览本地整理效果。"
                }, color = if (summaryState == "done") ink else secondary,
                    fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { summaryState = "generating" },
                        enabled = summaryState != "generating",
                        contentPadding = PaddingValues(0.dp)) {
                        Text(if (summaryState == "done") "重新生成" else "生成演示摘要",
                            color = teal, fontSize = 13.sp)
                    }
                    if (summaryState == "done") {
                        TextButton(onClick = { summary = ""; summaryState = "empty" }) {
                            Text("删除摘要", color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp)
                        }
                    }
                }
                Text("AI 生成 · 当前为本地界面演示，不调用服务，也不会扣除额度。",
                    color = secondary, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = {
            val transcript = buildString {
                appendLine(record.title)
                appendLine("${record.time} · ${record.duration}")
                appendLine()
                record.transcript.forEach { (speaker, content) ->
                    appendLine("$speaker：$content")
                }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, transcript)
            }
            context.startActivity(Intent.createChooser(intent, "分享会话文字"))
        }, shape = RoundedCornerShape(14.dp)) { Text("分享会话文字") }
        Spacer(Modifier.height(12.dp))
        if (record.isExample) {
            Text("以上是示例内容，不是真实识别结果。", color = secondary, fontSize = 12.sp)
        } else {
            Text("记录仅保存在本机，卸载应用可能清除记录。", color = secondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { newTitle = record.title; rename = true }) {
                    Text("重命名", color = teal)
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("删除此会话", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
