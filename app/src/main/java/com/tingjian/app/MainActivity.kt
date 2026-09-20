package com.tingjian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.ui.theme.TingjianTheme

// 所有记录和字幕均为本地示例；接入真实服务之前，不申请麦克风权限。
private val ink = Color(0xFF173138)
private val secondary = Color(0xFF60777D)
private val teal = Color(0xFF147D78)
private val deep = Color(0xFF163E41)
private val mint = Color(0xFFE7F6F1)
private val canvas = Color(0xFFF7FAF9)
private val divider = Color(0xFFE1EBE8)
private val white = Color(0xFFFFFFFF)

private data class Conversation(
    val title: String,
    val time: String,
    val preview: String,
    val duration: String,
    val transcript: List<Pair<String, String>>
)

private val examples = listOf(
    Conversation(
        "和朋友聊聊周末",
        "今天 14:32",
        "我们周六去公园走走，怎么样？",
        "12 分钟",
        listOf(
            "对方" to "我们周六去公园走走，怎么样？",
            "我" to "好呀，下午见！"
        )
    ),
    Conversation(
        "课堂笔记 · 人工智能",
        "昨天 09:15",
        "老师说，模型的训练需要关注数据质量。",
        "48 分钟",
        listOf(
            "老师" to "模型的训练需要关注数据质量。",
            "老师" to "下周请提交课程报告。"
        )
    ),
    Conversation(
        "家庭聚餐",
        "9 月 17 日",
        "下次见面一起做你最喜欢的菜。",
        "26 分钟",
        listOf(
            "家人" to "下次见面一起做你最喜欢的菜。",
            "我" to "好，我很期待。"
        )
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TingjianTheme(dynamicColor = false) {
                TingjianApp()
            }
        }
    }
}

@Composable
private fun TingjianApp() {
    var entered by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Conversation?>(null) }
    var running by remember { mutableStateOf(false) }
    var large by remember { mutableStateOf(false) }

    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    BackHandler(enabled = entered && selected != null) {
        selected = null
    }
    BackHandler(enabled = entered && selected == null && tab != 0) {
        tab = 0
    }

    if (!entered) {
        WelcomeScreen { entered = true }
        return
    }

    Scaffold(
        containerColor = canvas,
        bottomBar = {
            if (selected == null && !keyboardVisible) {
                BottomTabs(tab) { tab = it }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            val record = selected
            if (record != null) {
                DetailScreen(record, large) { selected = null }
            } else {
                when (tab) {
                    0 -> HomeScreen(
                        onNew = { tab = 1 },
                        onHistory = { tab = 2 },
                        onOpen = { selected = it }
                    )
                    1 -> LiveScreen(
                        running = running,
                        large = large,
                        onToggle = { running = !running }
                    )
                    2 -> HistoryScreen(onOpen = { selected = it })
                    else -> ProfileScreen(
                        large = large,
                        onLargeChange = { large = it },
                        onLeave = {
                            entered = false
                            tab = 0
                            running = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).background(teal, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("听", fontSize = 27.sp, color = white, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(11.dp))
        Text("听见", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = ink)
    }
}

@Composable
private fun Title(text: String, subtitle: String? = null) {
    Text(
        text,
        color = ink,
        fontSize = 27.sp,
        lineHeight = 35.sp,
        fontWeight = FontWeight.Bold
    )
    if (subtitle != null) {
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = secondary, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun Pill(text: String, highlighted: Boolean = false) {
    Surface(
        color = if (highlighted) mint else white,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (highlighted) mint else divider)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) teal else secondary
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = teal,
            contentColor = white
        )
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WelcomeScreen(onEnter: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(58.dp))
        BrandMark()
        Spacer(Modifier.height(55.dp))
        Pill("让交流更自由", highlighted = true)
        Spacer(Modifier.height(17.dp))
        Text(
            "听见每一句话。",
            color = ink,
            fontSize = 34.sp,
            lineHeight = 43.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Text(
            "让交流更轻松，让陪伴更贴近。",
            color = secondary,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(48.dp))
        Surface(
            color = white,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(23.dp)) {
                Box(
                    Modifier.size(45.dp).background(mint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("≋", fontSize = 26.sp, color = teal)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "欢迎使用听见",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "先体验实时字幕和会话记录的界面，稍后再连接账号。",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = secondary
                )
                Spacer(Modifier.height(22.dp))
                PrimaryButton("进入界面演示  →", onEnter)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "本地演示不访问麦克风，也不上传或保存个人信息。",
            modifier = Modifier.fillMaxWidth(),
            color = secondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(42.dp))
        Text(
            "听见 Tingjian  ·  让每一种交流都有回响",
            color = secondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeScreen(
    onNew: () -> Unit,
    onHistory: () -> Unit,
    onOpen: (Conversation) -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        BrandMark()
        Spacer(Modifier.height(34.dp))
        Title("你好，今天也要好好听。", "让重要的话，都被看见。")
        Spacer(Modifier.height(27.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = deep,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "✦  实时字幕",
                    fontSize = 13.sp,
                    color = Color(0xFFB7E9DE)
                )
                Spacer(Modifier.height(17.dp))
                Text(
                    "面对面交流，\n每一句都清晰。",
                    fontSize = 26.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold,
                    color = white
                )
                Spacer(Modifier.height(23.dp))
                Button(
                    onClick = onNew,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDDF8EF),
                        contentColor = deep
                    )
                ) {
                    Text("开始新会话   →", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "当前为字幕界面演示",
                    fontSize = 12.sp,
                    color = Color(0xFFB7E9DE)
                )
            }
        }
        Spacer(Modifier.height(29.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "最近的会话",
                color = ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onHistory) {
                Text("查看全部  →", color = teal, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
        examples.take(2).forEach {
            ConversationCard(it) { onOpen(it) }
            Spacer(Modifier.height(11.dp))
        }
        Text(
            "上方为示例记录，尚未连接服务器。",
            fontSize = 12.sp,
            color = secondary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConversationCard(item: Conversation, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = white,
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, divider)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(43.dp).background(mint, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("≋", color = teal, fontSize = 26.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    item.preview,
                    color = secondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "${item.time}  ·  ${item.duration}",
                    color = secondary,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("›", color = secondary, fontSize = 25.sp)
        }
    }
}

@Composable
private fun LiveScreen(
    running: Boolean,
    large: Boolean,
    onToggle: () -> Unit
) {
    var reply by remember { mutableStateOf("") }
    var shownReply by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("中英混合") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().imePadding()) {
        // 字幕单独滚动，输入框固定在底部，随键盘上移。
        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 23.dp)
        ) {
            Spacer(Modifier.height(27.dp))
            Title("实时字幕", "把身边的声音，变成看得见的文字。")
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("中英混合", "中文", "English").forEach { option ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (language == option) mint else white,
                        border = BorderStroke(
                            1.dp,
                            if (language == option) teal else divider
                        ),
                        modifier = Modifier.clickable { language = option }
                    ) {
                        Text(
                            option,
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            color = if (language == option) teal else secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                color = white,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(21.dp)) {
                    Text(
                        if (running) "●  字幕演示中" else "○  等待开始",
                        fontSize = 13.sp,
                        color = teal,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        if (running) {
                            "你好，很高兴见到你！\n今天天气真不错，我们一起出去走走吧。"
                        } else {
                            "开始演示后，对话文字会出现在这里。"
                        },
                        fontSize = if (large) 27.sp else 22.sp,
                        lineHeight = if (large) 41.sp else 34.sp,
                        color = ink,
                        fontWeight = FontWeight.Medium
                    )
                    if (shownReply.isNotEmpty()) {
                        Spacer(Modifier.height(22.dp))
                        HorizontalDivider(color = divider)
                        Spacer(Modifier.height(18.dp))
                        Text("我 · 文字回复", fontSize = 12.sp, color = teal)
                        Spacer(Modifier.height(9.dp))
                        Text(
                            shownReply,
                            color = ink,
                            fontSize = if (large) 22.sp else 17.sp
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        if (running) {
                            "示例字幕 · 没有使用麦克风"
                        } else {
                            "清晰易读 · 可以在“我的”调整字号"
                        },
                        color = secondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                if (running) "结束字幕演示" else "开始字幕演示",
                onToggle
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "这里展示的是固定示例，尚未接入录音或语音识别。",
                color = secondary,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(20.dp))
        }

        Surface(
            color = white,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 23.dp, vertical = 11.dp)) {
                Text(
                    "我的表达",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it.take(200) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    placeholder = { Text("输入你想说的话…") },
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = teal,
                        unfocusedBorderColor = divider,
                        focusedContainerColor = white,
                        unfocusedContainerColor = white
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        shownReply = reply.trim()
                        reply = ""
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    enabled = reply.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Text("显示我的文字")
                }
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

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(27.dp))
        Title("会话记录", "重要的对话，随时回来看看。")
        Spacer(Modifier.height(23.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("搜索示例会话") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = teal,
                unfocusedBorderColor = divider,
                focusedContainerColor = white,
                unfocusedContainerColor = white
            )
        )
        Spacer(Modifier.height(23.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "全部记录",
                color = ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "演示记录 · ${filtered.size} 条",
                color = secondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(15.dp))
        if (filtered.isEmpty()) {
            Surface(
                color = white,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "没有找到相关会话",
                    Modifier.padding(30.dp),
                    color = secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        filtered.forEach {
            ConversationCard(it) { onOpen(it) }
            Spacer(Modifier.height(11.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "目前只展示本地示例，尚未保存真实会话。",
            color = secondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun DetailScreen(
    record: Conversation,
    large: Boolean,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) {
            Text("←  返回", color = teal)
        }
        Spacer(Modifier.height(16.dp))
        Title(
            record.title,
            "${record.time}  ·  ${record.duration}  ·  示例会话"
        )
        Spacer(Modifier.height(29.dp))
        Text(
            "会话原文",
            color = ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(15.dp))
        record.transcript.forEach { (speaker, content) ->
            Surface(
                color = white,
                shape = RoundedCornerShape(19.dp),
                border = BorderStroke(1.dp, divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(19.dp)) {
                    Text(
                        speaker,
                        color = teal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        content,
                        color = ink,
                        fontSize = if (large) 23.sp else 17.sp,
                        lineHeight = if (large) 36.sp else 27.sp
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "以上内容仅供界面体验，不是真实识别结果。",
            color = secondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ProfileScreen(
    large: Boolean,
    onLargeChange: (Boolean) -> Unit,
    onLeave: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(27.dp))
        Title("我的", "按你的习惯，轻松交流。")
        Spacer(Modifier.height(25.dp))
        Surface(
            color = deep,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "你好，体验者",
                    color = white,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "正在使用本地界面演示",
                    color = Color(0xFFC1E9E0),
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(29.dp))
        Text(
            "阅读设置",
            color = ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(13.dp))
        Surface(
            color = white,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "大字字幕",
                        color = ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "在字幕与记录页面使用更大的文字",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = large,
                    onCheckedChange = onLargeChange
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "关于演示",
            color = ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(13.dp))
        Surface(
            color = white,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(19.dp)) {
                Text(
                    "听见 Tingjian",
                    color = ink,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "当前界面中的字幕和记录都是示例内容。登录、录音、识别和云端同步尚未接入。",
                    color = secondary,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
            }
        }
        Spacer(Modifier.height(21.dp))
        TextButton(onClick = onLeave) {
            Text("返回欢迎页", color = teal)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = white, shadowElevation = 7.dp) {
        Row(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 7.dp)
        ) {
            listOf("首页", "字幕", "记录", "我的").forEachIndexed { index, name ->
                Column(
                    Modifier.weight(1f)
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        listOf("⌂", "≋", "▤", "○")[index],
                        fontSize = 23.sp,
                        color = if (index == selected) teal else secondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        name,
                        fontSize = 11.sp,
                        fontWeight = if (index == selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (index == selected) teal else secondary
                    )
                }
            }
        }
    }
}