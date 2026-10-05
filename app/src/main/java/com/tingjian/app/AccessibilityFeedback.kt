package com.tingjian.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal enum class AccessibilityEvent {
    KEYWORD,
    CONNECTION_LOST
}

internal fun vibrationPattern(
    event: AccessibilityEvent,
    strong: Boolean
): LongArray = when (event) {
    AccessibilityEvent.KEYWORD -> if (strong) {
        longArrayOf(0L, 180L, 90L, 180L)
    } else {
        longArrayOf(0L, 100L)
    }
    AccessibilityEvent.CONNECTION_LOST -> if (strong) {
        longArrayOf(0L, 250L, 120L, 250L, 120L, 250L)
    } else {
        longArrayOf(0L, 160L, 100L, 160L)
    }
}

internal class AccessibilityFeedback(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID,
                "听见无障碍提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "关键词和连接状态的视觉提醒"
                enableVibration(false)
            })
        }
    }

    fun keyword(
        keyword: String,
        vibrationEnabled: Boolean,
        strongVibration: Boolean,
        notificationEnabled: Boolean
    ) {
        if (vibrationEnabled) vibrate(vibrationPattern(
            AccessibilityEvent.KEYWORD, strongVibration
        ))
        if (notificationEnabled) notify(
            KEYWORD_NOTIFICATION_ID,
            "关键词提醒",
            "字幕中出现：$keyword"
        )
    }

    fun connectionLost(notificationEnabled: Boolean) {
        if (notificationEnabled) notify(
            CONNECTION_NOTIFICATION_ID,
            "实时同步已中断",
            "消息将保存在本机，恢复网络后自动补发"
        )
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun notify(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(appContext, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(appContext)
        }
        notificationManager.notify(id, builder
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build())
    }

    private companion object {
        const val CHANNEL_ID = "tingjian_accessibility"
        const val KEYWORD_NOTIFICATION_ID = 4101
        const val CONNECTION_NOTIFICATION_ID = 4102
    }
}
