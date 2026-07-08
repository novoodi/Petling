package com.example.petling.domain.capture

/** OCR 텍스트에서 첫 URL을 뽑아 정규화한다. 순수 함수(테스트 가능). */
object UrlExtractor {

    private val urlRegex = Regex(
        """(https?://[^\s]+)|((www\.)?[a-z0-9][a-z0-9-]*\.(com|co\.kr|kr|net|org|io|be|me|app|dev|gg)(/[^\s]*)?)""",
        RegexOption.IGNORE_CASE,
    )

    /** 없으면 null. http(s)가 없으면 https:// 를 붙여 열 수 있게 한다. */
    fun firstUrl(text: String): String? {
        val raw = urlRegex.find(text)?.value?.trim()?.trimEnd('.', ',', ')', ']') ?: return null
        return if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
    }
}
