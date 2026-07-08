package com.example.petling.domain.capture

import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CaptureType

/**
 * 규칙 기반 캡처 분류기(전 기기 폴백). 순수 함수라 JVM 단위 테스트로 검증한다.
 *
 * 내부적으로 [CaptureType] 신호를 계산한 뒤, 사용자의 활성 카테고리([categories]) 중
 * 그 기반 종류(baseType)에 해당하는 카테고리 key로 매핑한다. 활성 집합에 없으면 catch-all로.
 *
 * 전략:
 * 1) 구조가 뚜렷한 종류는 하드 규칙으로 먼저 판정(대화/링크/일정).
 * 2) 나머지(공부/장소/쇼핑/링크 키워드)는 키워드 "점수제" — 가장 많이 맞는 종류 선택.
 */
class RuleBasedCaptureClassifier : CaptureClassifier {

    private val urlRegex = Regex("(https?://|www\\.)[^\\s]+", RegexOption.IGNORE_CASE)
    private val domainRegex = Regex("[a-z0-9-]+\\.(com|kr|net|co\\.kr|io|be)", RegexOption.IGNORE_CASE)
    private val moneyRegex = Regex("[₩]|\\d[\\d,]*\\s*원")
    private val timeStampRegex = Regex("(오전|오후)\\s*\\d{1,2}:\\d{2}")

    private val studyKeywords = listOf(
        "문제", "필기", "강의", "강의실", "강의계획서", "시험", "중간고사", "기말고사", "모의고사", "수능",
        "개념", "공식", "정답", "오답", "풀이", "단원", "챕터", "요약", "복습", "예습", "과제", "숙제",
        "리포트", "발표", "스터디", "등교", "출석", "결석", "수업", "수강신청", "수강", "학점", "과목",
        "전공", "교수", "교재", "교육비", "등록금", "장학금", "성적", "학사", "학과", "학번", "시간표",
        "종합정보시스템", "이러닝", "복학", "휴학", "재학", "학생", "논문",
    )
    private val placeKeywords = listOf(
        "영업시간", "영업", "오픈", "브레이크타임", "라스트오더", "주소", "지도", "길찾기", "네이버지도",
        "카카오맵", "구글지도", "리뷰", "후기", "평점", "별점", "맛집", "메뉴", "예약", "웨이팅", "주차",
        "전화번호", "위치", "도보", "저장하기",
    )
    private val shoppingKeywords = listOf(
        "결제", "주문", "배송", "배송비", "무료배송", "장바구니", "위시리스트", "합계", "총 금액", "구매",
        "영수증", "카드승인", "할인", "쿠폰", "적립", "품절", "재입고", "옵션", "수량", "쿠팡", "무신사",
        "지그재그", "에이블리", "올리브영", "스마트스토어", "네이버쇼핑",
    )
    private val linkKeywords = listOf(
        "링크", "기사", "뉴스", "블로그", "게시물", "게시글", "유튜브", "youtube", "인스타", "instagram",
        "틱톡", "tiktok", "채널", "구독", "조회수",
    )
    private val chatKeywords = listOf("읽음", "안읽음", "님이 들어왔", "님이 나갔", "채팅방", "이모티콘", "답장")

    override suspend fun classify(
        ocrText: String,
        parsedHasSchedule: Boolean,
        scheduleTitle: String?,
        categories: List<Category>,
    ): Classification {
        val text = ocrText.trim()
        if (text.isBlank()) return result(BuiltInCatalog.MEMORY, categories, "캡처", 0.2f)

        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val lineCount = lines.size

        // ── 하드 규칙(구조가 뚜렷한 종류) ──

        // 대화: 타임스탬프 여러 번 + 짧은 라인 다수 → 일정보다 먼저(카톡은 시각이 흔함)
        val timeStamps = timeStampRegex.findAll(text).count()
        val shortLines = lines.count { it.length <= 20 }
        val looksLikeChat = chatKeywords.any { text.contains(it) } ||
            (timeStamps >= 2 && lineCount >= 3 && shortLines >= lineCount / 2)
        if (looksLikeChat) keyFor(CaptureType.CHAT, categories)?.let {
            return Classification(it, autoTitle(text), 0.7f)
        }

        // 링크: URL/도메인
        if (urlRegex.containsMatchIn(text) || domainRegex.containsMatchIn(text)) {
            keyFor(CaptureType.LINK, categories)?.let { return Classification(it, autoTitle(text), 0.75f) }
        }

        // 일정: 날짜+시간이 있고 짧은 이벤트 메모일 때만
        if (parsedHasSchedule && lineCount <= 6) {
            keyFor(CaptureType.SCHEDULE, categories)?.let {
                val title = scheduleTitle?.ifBlank { autoTitle(text) } ?: autoTitle(text)
                return Classification(it, title, 0.8f)
            }
        }

        // ── 키워드 점수제(공부/장소/쇼핑/링크) — 활성 카테고리가 있는 종류만 ──
        val hasMoney = moneyRegex.containsMatchIn(text)
        val scores = listOf(
            CaptureType.STUDY to countHits(text, studyKeywords),
            CaptureType.PLACE to countHits(text, placeKeywords),
            CaptureType.SHOPPING to countHits(text, shoppingKeywords) + if (hasMoney) 2 else 0,
            CaptureType.LINK to countHits(text, linkKeywords),
        ).filter { keyFor(it.first, categories) != null }
        val best = scores.maxByOrNull { it.second }
        if (best != null && best.second > 0) {
            val confidence = (0.45f + 0.1f * best.second).coerceAtMost(0.85f)
            return Classification(keyFor(best.first, categories)!!, autoTitle(text), confidence)
        }

        // 폴백: 추억/기타(catch-all)
        return result(BuiltInCatalog.MEMORY, categories, autoTitle(text), 0.3f)
    }

    /** 서로 다른 키워드가 몇 개 맞았는지(반복은 1로) 센다. */
    private fun countHits(text: String, keywords: List<String>): Int =
        keywords.count { text.contains(it, ignoreCase = true) }

    private fun autoTitle(text: String): String = CaptureTitle.autoTitle(text)

    /** baseType에 해당하는 활성 카테고리 key(빌트인 우선), 없으면 null. */
    private fun keyFor(baseType: CaptureType, categories: List<Category>): String? {
        val matches = categories.filter { it.baseType == baseType }
        return (matches.firstOrNull { it.isBuiltIn } ?: matches.firstOrNull())?.key
    }

    /** 지정 key가 활성이면 그 key로, 아니면 첫 활성 카테고리로 결과 생성. */
    private fun result(preferredKey: String, categories: List<Category>, title: String, conf: Float): Classification {
        val key = categories.firstOrNull { it.key == preferredKey }?.key
            ?: categories.firstOrNull()?.key
            ?: BuiltInCatalog.MEMORY
        return Classification(key, title, conf)
    }
}
