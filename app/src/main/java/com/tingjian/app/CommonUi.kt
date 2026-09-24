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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

internal val ink = Color(0xFF173138)
internal val secondary = Color(0xFF60777D)
internal val teal = Color(0xFF147D78)
internal val deep = Color(0xFF163E41)
internal val mint = Color(0xFFE7F6F1)
internal val canvas = Color(0xFFF7FAF9)
internal val divider = Color(0xFFE1EBE8)
internal val white = Color(0xFFFFFFFF)

@Composable
internal fun BrandMark() {
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
internal fun Title(text: String, subtitle: String? = null) {
    Text(text, color = ink, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
    if (subtitle != null) {
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = secondary, fontSize = 16.sp, lineHeight = 23.sp)
    }
}

@Composable
internal fun Pill(text: String, highlighted: Boolean = false) {
    Surface(color = if (highlighted) mint else white, shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (highlighted) mint else divider)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (highlighted) teal else secondary)
    }
}

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = white)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ConversationCard(item: Conversation, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        // 合并卡片内部文本，TalkBack 一次即可读出标题、摘要和时间。
        color = white, shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, divider)) {
        Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {
            contentDescription = "${item.title}，${item.preview}，${item.time}"
        }, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(mint, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) { Text("≋", color = teal, fontSize = 26.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink)
                Spacer(Modifier.height(5.dp))
                Text(item.preview, color = secondary, fontSize = 15.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text("${item.time}  ·  ${item.duration}", color = secondary, fontSize = 11.sp)
            }
            if (item.isExample) {
                Spacer(Modifier.width(6.dp))
                Text("示例", color = teal, fontSize = 11.sp,
                    modifier = Modifier.background(mint, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp))
            } else if (item.syncPending) {
                Spacer(Modifier.width(6.dp))
                Text("待同步", color = Color(0xFF875B00), fontSize = 11.sp,
                    modifier = Modifier.background(Color(0xFFFFF1C2), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text("›", color = secondary, fontSize = 25.sp)
        }
    }
}

@Composable
internal fun ChatBubble(text: String, fromMe: Boolean, large: Boolean,
    inProgress: Boolean = false, speaker: String? = null, onReplay: (() -> Unit)? = null,
    keywords: List<String> = emptyList()) {
    val highlighted = buildAnnotatedString {
        append(text)
        keywords.filter { it.isNotBlank() }.forEach { keyword ->
            var start = text.indexOf(keyword, ignoreCase = true)
            while (start >= 0) {
                addStyle(SpanStyle(background = Color(0xFFFFE5A3),
                    fontWeight = FontWeight.Bold), start, start + keyword.length)
                start = text.indexOf(keyword, start + keyword.length, ignoreCase = true)
            }
        }
    }
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start) {
        Column(Modifier.fillMaxWidth(0.84f),
            horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start) {
            Text(speaker ?: if (fromMe) (if (onReplay != null) "我 · 点击重播" else "我")
                else if (inProgress) "正在识别" else "对方",
                color = secondary, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp))
            Surface(color = if (fromMe) mint else white,
                shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, divider),
                modifier = if (onReplay == null) Modifier else Modifier.clickable(onClick = onReplay)) {
                Text(highlighted, Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                    color = ink, fontSize = if (large) 23.sp else 17.sp,
                    lineHeight = if (large) 34.sp else 26.sp)
            }
        }
    }
}

@Composable
internal fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(color = white, shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, divider),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 17.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title，$subtitle"
            },
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = secondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Text("›", color = secondary, fontSize = 25.sp)
        }
    }
}

@Composable
internal fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = white, shadowElevation = 7.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp, bottom = 7.dp)) {
            listOf("首页", "字幕", "记录", "我的").forEachIndexed { index, name ->
                Column(Modifier.weight(1f).clickable { onSelect(index) }
                    .semantics(mergeDescendants = true) { contentDescription = "$name 标签" }
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
