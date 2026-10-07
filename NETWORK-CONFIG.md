# Android 网络与真机联调

## 地址配置

模拟器默认使用：

```text
http://10.0.2.2:8088/
```

真机和电脑连接同一个 Wi-Fi 后，在 `gradle.properties` 中配置电脑的局域网地址：

```text
TINGJIAN_API_BASE_URL=http://192.168.1.10:8088/
```

使用 `ipconfig` 查看电脑 IPv4 地址，并允许 Windows 防火墙放行网关的 `8088` 端口。
正式发布必须使用 HTTPS 地址，不能依赖 debug 清单中的明文 HTTP 设置。

Android 统一访问 `gateway-service`。`8080` 是单体内部入口，`8092` 和 `8093` 是
AI/语音及用量服务入口，不应写入 Android 配置。需要临时排查单体时才可显式设置
`TINGJIAN_API_BASE_URL=http://10.0.2.2:8080/`，设置页会显示“单体直连”。

修改 `gradle.properties` 后需要重新构建应用：

```powershell
.\gradlew.bat testDebugUnitTest --no-daemon
```

## 错误处理

客户端区分超时、断网、TLS、登录失效、限流和服务端错误。超时、断网、限流以及
5xx 错误标记为可重试；协程取消不会被误报成网络失败。每次请求携带 `X-Request-Id`，
服务端也会在响应与日志中返回相同编号，方便定位问题。
