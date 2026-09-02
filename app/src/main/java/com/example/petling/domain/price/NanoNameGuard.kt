package com.example.petling.domain.price

/**
 * Nano가 돌려준 상품명을 채택할지 판단하는 순수 규칙.
 *
 * 실기기 QA(폴드6, 2026-09-02): "존쿡 델리미트 캠핑파티" 가격표에서 Nano가 "존국리트핑파다 840g"을 답했는데,
 * 옛 검사(토큰 하나라도 OCR에 있으면 통과)는 "840g" 때문에 통과시켰다. 숫자·용량 토큰은 증거가 안 된다.
 */
object NanoNameGuard {

    private val VOLUME_TOKEN = Regex("""(?i)^\d+(?:[.,]\d+)?\s*(?:kg|g|ml|l|매|입|개|팩|병|캔|구|p)$""")

    /** 용량·수량 토큰("840g", "5입")을 제거한 이름. */
    fun stripVolumeTokens(name: String): String =
        name.split(' ').filter { it.isNotBlank() && !VOLUME_TOKEN.matches(it) }.joinToString(" ").trim()

    /**
     * Nano 이름을 쓸 수 있는지: 용량 토큰을 뺀 뒤, **한글이 2자 이상인 토큰**이 OCR 텍스트에 그대로 들어 있어야 한다.
     * 한 글자 우연 일치·숫자 일치는 배제한다.
     */
    fun isSupportedByOcr(nanoName: String, ocrText: String): Boolean {
        val compactOcr = ocrText.filter { !it.isWhitespace() }
        return stripVolumeTokens(nanoName).split(' ').any { token ->
            token.count { it in '가'..'힣' } >= 2 && compactOcr.contains(token)
        }
    }

    /** 이름이 이미 용량을 담고 있는지(목록·상세에서 용량을 덧붙일지 결정). */
    fun containsVolume(name: String): Boolean =
        name.split(' ').any { VOLUME_TOKEN.matches(it) }
}
