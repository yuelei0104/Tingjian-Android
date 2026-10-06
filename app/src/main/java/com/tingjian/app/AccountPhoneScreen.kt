package com.tingjian.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tingjian.app.data.ApiResult
import com.tingjian.app.network.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun AccountPhoneScreen(onBack: () -> Unit) {
    val repository = remember { NetworkModule.repository }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var boundPhone by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var cooldown by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    suspend fun reload() {
        loading = true
        when (val result = repository.accountPhone()) {
            is ApiResult.Success -> boundPhone = result.value.maskedPhone
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }
    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onBack) { Text("←  返回", color = teal) }
        Spacer(Modifier.height(14.dp))
        Title("手机号安全", "绑定后可使用短信验证码找回密码。")
        Spacer(Modifier.height(20.dp))
        if (loading) {
            CircularProgressIndicator(color = teal)
        } else {
            Surface(
                color = white,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(boundPhone?.let { "已绑定：$it" } ?: "尚未绑定手机号", color = ink)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filterNot(Char::isWhitespace).take(16) },
                        label = { Text(if (boundPhone == null) "手机号" else "新手机号") },
                        supportingText = { Text("请包含国家区号，例如 +8613800138000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            label = { Text("短信验证码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            enabled = !busy && cooldown == 0,
                            onClick = {
                                if (!phone.startsWith('+') || phone.length !in 9..16) {
                                    error = "请填写带国家区号的手机号"
                                    return@OutlinedButton
                                }
                                busy = true
                                error = ""
                                scope.launch {
                                    when (val result = repository.requestPhoneBindingCode(phone)) {
                                        is ApiResult.Success -> {
                                            verificationId = result.value.verificationId
                                            notice = "验证码已发送"
                                            cooldown = 60
                                            scope.launch {
                                                while (cooldown > 0) {
                                                    delay(1_000)
                                                    cooldown--
                                                }
                                            }
                                        }
                                        is ApiResult.Error -> error = result.message
                                    }
                                    busy = false
                                }
                            }
                        ) {
                            Text(if (cooldown > 0) "${cooldown}s" else "发送验证码")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = !busy && verificationId.isNotBlank() && code.length == 6,
                        onClick = {
                            busy = true
                            error = ""
                            scope.launch {
                                when (val result = repository.bindPhone(
                                    phone, verificationId, code
                                )) {
                                    is ApiResult.Success -> {
                                        boundPhone = result.value.maskedPhone
                                        notice = "手机号绑定成功"
                                        phone = ""
                                        code = ""
                                        verificationId = ""
                                    }
                                    is ApiResult.Error -> error = result.message
                                }
                                busy = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = teal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "处理中…" else if (boundPhone == null) "绑定手机号" else "换绑手机号") }
                    if (boundPhone != null) {
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(128) },
                            label = { Text("当前密码") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            enabled = !busy && password.length >= 8,
                            onClick = {
                                busy = true
                                error = ""
                                scope.launch {
                                    when (val result = repository.unbindPhone(password)) {
                                        is ApiResult.Success -> {
                                            boundPhone = null
                                            password = ""
                                            notice = "手机号已解绑"
                                        }
                                        is ApiResult.Error -> error = result.message
                                    }
                                    busy = false
                                }
                            }
                        ) {
                            Text("解绑手机号", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (notice.isNotBlank()) Text(notice, color = teal)
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
