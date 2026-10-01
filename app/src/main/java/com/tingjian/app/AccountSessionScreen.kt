package com.tingjian.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tingjian.app.network.AccountSessionResponse

@Composable
internal fun AccountSessionScreen(
    sessions: List<AccountSessionResponse>,
    loading: Boolean,
    error: String,
    revokingId: String?,
    onRetry: () -> Unit,
    onRevoke: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(14.dp))
        Title("登录会话", "查看仍可访问账号的登录记录，并撤销不再使用的会话。")
        Spacer(Modifier.height(20.dp))
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = teal)
                Text("正在读取登录会话…", color = secondary,
                    modifier = Modifier.padding(start = 12.dp))
            }
        } else if (error.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = onRetry) { Text("重试", color = teal) }
                }
            }
        } else if (sessions.isEmpty()) {
            Text("当前没有可管理的登录会话。", color = secondary)
        } else {
            sessions.forEachIndexed { index, session ->
                Surface(
                    color = white,
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Text("登录会话 ${index + 1}", color = ink,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("最近活动：${displayTime(session.lastActiveAt)}",
                            color = secondary, fontSize = 12.sp)
                        Text("登录时间：${displayTime(session.createdAt)}",
                            color = secondary, fontSize = 12.sp)
                        Text("有效期至：${displayTime(session.expiresAt)}",
                            color = secondary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onRevoke(session.id) },
                            enabled = revokingId == null
                        ) {
                            Text(if (revokingId == session.id) "正在撤销…" else "撤销此会话",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun displayTime(value: String): String = value
    .replace('T', ' ')
    .removeSuffix("Z")
    .take(19)
