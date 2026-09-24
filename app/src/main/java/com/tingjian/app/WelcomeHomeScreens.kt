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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
internal fun WelcomeScreen(onEnter: () -> Unit) {
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
        Text("试用单句语音转文字、文字播报，结束后可在本机查看会话。",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(records: List<Conversation>, dashboard: HomeDashboard?,
    refreshing: Boolean, homeError: String, hasDraft: Boolean, demoLoggedIn: Boolean,
    onNew: () -> Unit,
    onScene: (String) -> Unit, onHistory: () -> Unit, onUsage: () -> Unit,
    onLogin: () -> Unit, onRefresh: () -> Unit,
    onOpen: (Conversation) -> Unit) {
    val online = isNetworkAvailable(LocalContext.current)
    val localRecent = records.filterNot { it.isExample }.ifEmpty { records }
    val recent = dashboard?.recentConversations?.ifEmpty { localRecent } ?: localRecent
    val scenes = dashboard?.scenes?.takeIf { it.isNotEmpty() }
        ?: listOf("课堂", "会议", "就医", "日常")
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh) {
      Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
          .padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(24.dp))
        BrandMark()
        Spacer(Modifier.height(34.dp))
        Title("你好，今天也要好好听。", "让重要的话，都被看见。")
        if (!online) {
            Spacer(Modifier.height(12.dp))
            Surface(color = Color(0xFFFFF2C7), shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()) {
                Text("当前网络不可用；仍可输入文字，语音服务可能无法使用。",
                    Modifier.padding(13.dp), color = ink, fontSize = 14.sp)
            }
        }
        if (demoLoggedIn && dashboard != null) {
            Spacer(Modifier.height(12.dp))
            Surface(color = mint, shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()) {
                val minutes = dashboard.totalDurationSeconds / 60
                Text("${dashboard.conversationCount} 段会话  ·  " +
                    "${dashboard.messageCount} 条文字  ·  ${minutes} 分钟",
                    Modifier.padding(13.dp), color = deep, fontSize = 13.sp)
            }
        }
        if (demoLoggedIn && homeError.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(color = Color(0xFFFFF2C7), shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(homeError, Modifier.weight(1f), color = ink, fontSize = 13.sp)
                    TextButton(onClick = onRefresh) { Text("重试", color = teal) }
                }
            }
        }
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
                    Text(if (hasDraft) "继续会话   →" else "开始新会话   →", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("语音转文字  ·  大字展示  ·  文字播报", fontSize = 12.sp, color = Color(0xFFB7E9DE))
            }
        }
        Spacer(Modifier.height(23.dp))
        Text("常用场景", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        scenes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    Surface(color = white, shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, divider),
                        modifier = Modifier.weight(1f).clickable { onScene(option) }) {
                        Text(option, Modifier.padding(vertical = 11.dp), color = ink,
                            textAlign = TextAlign.Center, fontSize = 15.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = white, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, divider),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onUsage)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("用量与额度", color = ink, fontWeight = FontWeight.SemiBold)
                    Text(dashboard?.let {
                        "${it.planName} · ${it.planDescription}"
                    } ?: "V1 内测演示 · 暂未开放购买",
                        color = secondary, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("›", color = teal, fontSize = 23.sp)
            }
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("最近的会话", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onHistory) { Text("查看全部  →", color = teal, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(9.dp))
        recent.take(2).forEach { ConversationCard(it) { onOpen(it) }; Spacer(Modifier.height(11.dp)) }
        if (recent.firstOrNull()?.isExample == true) {
            Text("标有“示例”的会话仅供体验。", fontSize = 12.sp, color = secondary)
        }
        Spacer(Modifier.height(12.dp))
        if (!demoLoggedIn) {
            TextButton(onClick = onLogin) {
                Text("登录或注册  →", color = teal, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
      }
    }
}
