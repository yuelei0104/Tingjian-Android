package com.tingjian.app

import com.tingjian.app.network.AiContextMessageRequest
import java.util.Locale

internal object AiAction {
    const val REPLY = "REPLY"
    const val POLITE = "POLITE"
    const val CONCISE = "CONCISE"
    const val FORMAL = "FORMAL"
    const val TRANSLATE_ZH = "TRANSLATE_ZH"
    const val TRANSLATE_EN = "TRANSLATE_EN"
}

internal fun buildAiContext(lines: List<ChatLine>): List<AiContextMessageRequest> =
    lines.takeLast(8).map { line ->
        AiContextMessageRequest(
            speaker = if (line.fromMe) "SELF" else "OTHER",
            content = line.content.trim().take(240)
        )
    }.filter { it.content.isNotBlank() }

internal fun localExpressionSuggestion(
    source: String,
    action: String,
    context: List<AiContextMessageRequest>
): String {
    val clean = source.trim()
    return when (action) {
        AiAction.REPLY -> {
            val latest = context.lastOrNull { it.speaker == "OTHER" }?.content.orEmpty()
            if (latest.contains('?') || latest.contains('？')) "好的，我确认后尽快回复您。"
            else if (latest.isBlank()) "好的，我明白了。" else "好的，我已了解，谢谢。"
        }
        AiAction.POLITE -> when {
            clean.isBlank() -> "麻烦您再说明一下，谢谢。"
            clean.endsWith("谢谢。") || clean.endsWith("谢谢！") -> clean
            else -> clean.replace("你", "您") + " 谢谢。"
        }
        AiAction.CONCISE -> {
            if (clean.isBlank()) "好的。"
            else clean.split(Regex("[。！？!?；;\\n]"), limit = 2).first().trim() +
                if (clean.any { it in '\u4e00'..'\u9fff' }) "。" else "."
        }
        AiAction.FORMAL -> if (clean.isBlank()) "我已了解相关内容。" else "关于此事，我的回复是：$clean"
        AiAction.TRANSLATE_ZH -> when (clean.lowercase(Locale.ROOT)) {
            "hello" -> "你好"
            "thank you", "thanks" -> "谢谢"
            "please wait" -> "请稍等"
            else -> if (clean.isBlank()) "请输入需要翻译的内容" else clean
        }
        AiAction.TRANSLATE_EN -> when (clean) {
            "你好" -> "Hello"
            "谢谢" -> "Thank you"
            "请稍等" -> "Please wait"
            else -> if (clean.isBlank()) "Please enter text to translate" else clean
        }
        else -> clean
    }
}
