package com.tingjian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme

private val ink = Color(0xFF152C32)
private val muted = Color(0xFF61767A)
private val teal = Color(0xFF127C76)
private val pale = Color(0xFFE6F5F1)
private val canvas = Color(0xFFF6F9F7)
private val line = Color(0xFFE5ECE9)

private data class Conversation(val title: String, val date: String, val preview: String, val duration: String)
private val samples = listOf(
    Conversation("和朋友聊聊周末", "今天 14:32", "我们周六去公园走走，怎么样？", "12 分钟"),
    Conversation("课堂笔记 · 人工智能", "昨天 09:15", "老师说，模型的训练需要关注数据质量。", "48 分钟"),
    Conversation("家庭聚餐", "9 月 17 日", "下次见面一起做你最喜欢的菜。", "26 分钟")
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
    var signedIn by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Conversation?>(null) }
    var live by remember { mutableStateOf(false) }
    var largerText by remember { mutableStateOf(false) }
    if (!signedIn) {
        LoginScreen { signedIn = true }
        return
    }
    Scaffold(containerColor = canvas, bottomBar = {
        if (selected == null) BottomTabs(tab) { tab = it; live = false }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val conversation = selected
            if (conversation != null) {
                ConversationScreen(conversation, largerText) { selected = null }
            } else when (tab) {
                0 -> HomeScreen(onNew = { tab = 1; live = true }, onOpen = { selected = it })
                1 -> LiveScreen(live, largerText) { live = !live }
                else -> ProfileScreen(largerText, onSizeChange = { largerText = it }, onSignOut = {
                    signedIn = false; tab = 0; selected = null; live = false
                })
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(55.dp).background(teal, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Text("听", fontSize = 29.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text("听见", fontSize = 25.sp, color = ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoginScreen(onEnter: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(canvas).verticalScroll(rememberScrollState())
        .padding(horizontal = 30.dp, vertical = 55.dp)) {
        BrandMark()
        Spacer(Modifier.height(54.dp))
        Text("听见每一句话。", fontSize = 33.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(12.dp))
        Text("让交流更轻松，让陪伴更贴近。", fontSize = 16.sp, color = muted)
        Spacer(Modifier.height(72.dp))
        Text("欢迎使用听见", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(10.dp))
        Text("先看看应用的样子，稍后再连接账号。", color = muted, fontSize = 14.sp)
        Spacer(Modifier.height(25.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("手机号（演示阶段可留空）") }, shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(18.dp))
        PrimaryButton("进入演示", onEnter)
        Spacer(Modifier.height(12.dp))
        Text("当前为本地界面演示，不会发送验证码或保存号码。", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(90.dp))
        Text("听见 Tingjian  ·  让每一种交流都有回响", color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun HomeScreen(onNew: () -> Unit, onOpen: (Conversation) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(16.dp))
        BrandMark()
        Spacer(Modifier.height(33.dp))
        Text("你好，今天也要好好听。", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(8.dp))
        Text("让重要的话，都被看见。", fontSize = 15.sp, color = muted)
        Spacer(Modifier.height(28.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF123F42))) {
            Column(Modifier.padding(24.dp)) {
                Text("✦  实时字幕", fontSize = 14.sp, color = Color(0xFFADE6D8))
                Spacer(Modifier.height(17.dp))
                Text("面对面交流，\n每一句都清晰。", fontSize = 25.sp, lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(21.dp))
                Button(onClick = onNew, colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD3F4E7), contentColor = ink)) {
                    Text("开始新会话  →", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("最近的会话", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = ink)
            Text("演示记录", fontSize = 12.sp, color = muted)
        }
        Spacer(Modifier.height(14.dp))
        samples.forEach { conversation ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { onOpen(conversation) }, color = Color.White,
                shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, line)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(42.dp).background(pale, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) { Text("≋", fontSize = 26.sp, color = teal) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(conversation.title, fontWeight = FontWeight.SemiBold, color = ink, fontSize = 15.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(conversation.preview, color = muted, fontSize = 13.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Text("${conversation.date}  ·  ${conversation.duration}", color = muted, fontSize = 11.sp)
                    }
                    Text("›", fontSize = 25.sp, color = muted)
                }
            }
            Spacer(Modifier.height(11.dp))
        }
    }
}

@Composable
private fun LiveScreen(live: Boolean, largerText: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("实时字幕", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(6.dp))
        Text("把身边的声音，变成看得见的文字。", fontSize = 14.sp, color = muted)
        Spacer(Modifier.height(29.dp))
        Surface(color = if (live) pale else Color.White, shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, line)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text(if (live) "●  演示进行中" else "○  准备开始", color = teal,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(19.dp))
                Text(if (live) "你好，很高兴见到你！\n今天天气真不错，我们一起出去走走吧。"
                    else "开始后，对话文字会显示在这里。",
                    fontSize = if (largerText) 26.sp else 22.sp,
                    lineHeight = if (largerText) 39.sp else 34.sp, color = ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(23.dp))
                Text(if (live) "示例字幕  ·  未使用麦克风" else "清晰易读  ·  支持大字模式",
                    color = muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(if (live) "结束演示" else "开始字幕演示", onToggle)
        Spacer(Modifier.height(14.dp))
        Text("这是界面演示，尚未接入录音、语音识别或自动保存。", fontSize = 12.sp, color = muted)
    }
}

@Composable
private fun ConversationScreen(conversation: Conversation, largerText: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("←  返回会话", color = teal) }
        Spacer(Modifier.height(21.dp))
        Text(conversation.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(6.dp))
        Text("${conversation.date}  ·  ${conversation.duration}", color = muted, fontSize = 13.sp)
        Spacer(Modifier.height(29.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, line)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("会话内容", color = teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(18.dp))
                Text(conversation.preview, fontSize = if (largerText) 25.sp else 20.sp,
                    lineHeight = if (largerText) 38.sp else 31.sp, color = ink)
                Spacer(Modifier.height(17.dp))
                Text("以上为演示内容", color = muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProfileScreen(largerText: Boolean, onSizeChange: (Boolean) -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("我的", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(24.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, line)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("你好，体验者", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink)
                Spacer(Modifier.height(7.dp))
                Text("当前使用本地演示模式", color = muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("阅读设置", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(13.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, line)) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("大字字幕", color = ink, fontWeight = FontWeight.SemiBold)
                    Text("让字幕更容易阅读", color = muted, fontSize = 12.sp)
                }
                Switch(checked = largerText, onCheckedChange = onSizeChange)
            }
        }
        Spacer(Modifier.height(26.dp))
        TextButton(onClick = onSignOut) { Text("返回欢迎页", color = teal) }
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("首页", "字幕", "我的").forEachIndexed { index, label ->
                Column(Modifier.weight(1f).clickable { onSelect(index) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(listOf("⌂", "≋", "◯")[index], fontSize = 24.sp,
                        color = if (selected == index) teal else muted)
                    Text(label, fontSize = 12.sp,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected == index) teal else muted)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = teal), shape = RoundedCornerShape(16.dp)) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun TingjianPreview() {
    TingjianTheme(dynamicColor = false) { TingjianApp() }
}
