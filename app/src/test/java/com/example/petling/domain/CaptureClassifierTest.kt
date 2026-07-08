package com.example.petling.domain

import com.example.petling.domain.capture.RuleBasedCaptureClassifier
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.CaptureType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureClassifierTest {

    private val classifier = RuleBasedCaptureClassifier()

    // 규칙은 기본 7종 활성 카테고리 안에서 분류한다. 빌트인 key == CaptureType.name.
    private val cats = BuiltInCatalog.defaults

    private fun keyOf(text: String, hasSchedule: Boolean = false, scheduleTitle: String? = null): String =
        runBlocking { classifier.classify(text, hasSchedule, scheduleTitle, cats).categoryKey }

    @Test
    fun schedule_wins_when_parser_found_datetime() {
        val c = runBlocking { classifier.classify("3월 15일 오후 3시 학원 상담", parsedHasSchedule = true, scheduleTitle = "상담", cats) }
        assertEquals(CaptureType.SCHEDULE.name, c.categoryKey)
        assertEquals("상담", c.title)
    }

    @Test
    fun link_by_url() {
        assertEquals(CaptureType.LINK.name, keyOf("이거 봐봐 https://youtube.com/watch?v=abc 개웃김"))
    }

    @Test
    fun link_by_keyword() {
        assertEquals(CaptureType.LINK.name, keyOf("오늘 뉴스 기사\n경제 전망 어쩌구"))
    }

    @Test
    fun shopping_by_money_and_keyword() {
        assertEquals(CaptureType.SHOPPING.name, keyOf("주문 완료\n상품 금액 32,000원\n배송 준비중"))
    }

    @Test
    fun place_by_keyword() {
        assertEquals(CaptureType.PLACE.name, keyOf("강남 스타벅스\n영업시간 07:00 - 22:00\n리뷰 4.5"))
    }

    @Test
    fun bare_location_noun_is_not_place() {
        // 흔한 명사(학원 등)만으로는 장소가 아니다 — 일정 오분류 방지의 반대면
        assertEquals(CaptureType.MEMORY.name, keyOf("우리동네 요가학원"))
    }

    @Test
    fun chat_with_datetime_is_chat_not_schedule() {
        // 카톡 로그는 날짜+시간이 있어도 대화로(일정보다 먼저 판정)
        val kakao = """
            민준
            오후 2:14 내일 3시에 보자
            지수
            오후 2:15 ㅇㅋ 어디서
            민준
            오후 2:16 강남역
        """.trimIndent()
        assertEquals(CaptureType.CHAT.name, runBlocking { classifier.classify(kakao, parsedHasSchedule = true, scheduleTitle = "보자", cats).categoryKey })
    }

    @Test
    fun long_text_with_datetime_is_not_schedule() {
        // 날짜+시간이 있어도 라인이 많으면(게시물/공지 등) 일정으로 단정하지 않는다
        val longPost = (1..10).joinToString("\n") { "게시물 내용 줄 $it 입니다 어쩌구저쩌구" } + "\n3월 15일 오후 3시"
        assertEquals(false, runBlocking { classifier.classify(longPost, parsedHasSchedule = true, scheduleTitle = null, cats).categoryKey } == CaptureType.SCHEDULE.name)
    }

    @Test
    fun no_datetime_is_not_schedule() {
        // 날짜+시간이 없으면 일정 아님(제목만 있는 캡처는 추억/기타)
        assertEquals(CaptureType.MEMORY.name, runBlocking { classifier.classify("친구랑 찍은 사진", parsedHasSchedule = false, scheduleTitle = null, cats).categoryKey })
    }

    @Test
    fun tuition_notice_is_study() {
        // 등록금/학사 안내는 공부(학사)로 — 예전엔 날짜 때문에 일정, 그다음엔 추억으로 샜던 케이스
        val notice = "종합정보시스템 → 등록금 → 등록금고지서 출력\n재입학생 및 복학자는 신청완료 후 교육비납입증명서 출력 가능"
        assertEquals(CaptureType.STUDY.name, keyOf(notice))
    }

    @Test
    fun course_registration_is_study() {
        assertEquals(CaptureType.STUDY.name, keyOf("2학기 수강신청 안내\n학점 및 시간표 확인"))
    }

    @Test
    fun shopping_app_is_shopping() {
        assertEquals(CaptureType.SHOPPING.name, keyOf("무신사 장바구니\n후드티 45,000원\n무료배송"))
    }

    @Test
    fun kakao_map_is_place() {
        assertEquals(CaptureType.PLACE.name, keyOf("스타벅스 강남점\n영업시간 매일 07:00\n주차 가능\n리뷰 1,204"))
    }

    @Test
    fun scoring_picks_dominant_category() {
        // 공부 키워드가 장소 키워드보다 많으면 공부
        assertEquals(CaptureType.STUDY.name, keyOf("중간고사 시험 범위\n개념 정리 필기\n근처 카페에서 공부"))
    }

    @Test
    fun chat_by_timestamps() {
        val kakao = """
            민준
            오후 2:14 야 오늘 뭐함
            지수
            오후 2:15 몰라 집
            민준
            오후 2:15 나가자
        """.trimIndent()
        assertEquals(CaptureType.CHAT.name, keyOf(kakao))
    }

    @Test
    fun study_by_keyword() {
        assertEquals(CaptureType.STUDY.name, keyOf("2단원 이차방정식 개념 정리\n근의 공식 필기"))
    }

    @Test
    fun memory_fallback() {
        assertEquals(CaptureType.MEMORY.name, keyOf("그냥 예쁜 하늘 사진 설명 같은 거"))
    }

    @Test
    fun blank_text_is_memory() {
        assertEquals(CaptureType.MEMORY.name, keyOf("   "))
    }

    @Test
    fun title_is_truncated() {
        val c = runBlocking {
            classifier.classify(
                "이것은 아주 긴 첫 번째 줄이고 제목으로 잘려야 하는 텍스트입니다 정말 길어요",
                parsedHasSchedule = false,
                scheduleTitle = null,
                cats,
            )
        }
        assertTrue(c.title.length <= 21) // 20자 + …
    }

    @Test
    fun priority_url_over_study() {
        // URL이 있으면 공부 키워드가 있어도 LINK 우선
        assertEquals(CaptureType.LINK.name, keyOf("시험 공부 자료 https://example.com/notes"))
    }

    @Test
    fun disabled_type_falls_back_to_catch_all() {
        // LINK를 끈 사용자면 URL이어도 catch-all(MEMORY)로 — 활성 집합만 사용
        val noLink = cats.filter { it.key != CaptureType.LINK.name }
        val key = runBlocking { classifier.classify("https://example.com 좋은 링크", false, null, noLink).categoryKey }
        assertEquals(CaptureType.MEMORY.name, key)
    }
}
