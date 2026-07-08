package com.example.petling.domain.model

/**
 * 사용자 맞춤 분류 카테고리.
 *
 * - [key]: 저장·식별용 안정 키. 빌트인은 [CaptureType] 이름과 동일("STUDY" 등), 커스텀은 "c_<n>".
 * - [baseType]: 동작(분기·색·일정 특례·규칙 점수)을 결정하는 기반 종류. 커스텀도 하나를 물려받아
 *   기존 성장/색 로직을 그대로 재사용한다.
 * - [enabled]: 사용자의 활성 집합에 포함되는지. Nano 프롬프트/필터/변경 칩은 활성 집합만 사용.
 */
data class Category(
    val key: String,
    val label: String,
    val emoji: String,
    val description: String,
    val baseType: CaptureType,
    val isBuiltIn: Boolean,
    val enabled: Boolean,
    val sortOrder: Int,
) {
    /** 배지·칩 표시용. 이모지 + 이름. */
    val display: String get() = "$emoji $label"
}

/**
 * 빌트인 카테고리 목록. 기본 ON 7종 + 사용자가 켜서 쓸 수 있는 후보(기본 OFF).
 * 순수 데이터라 도메인에 둔다(안드로이드 의존 없음). 최초 실행 시 DB에 시드된다.
 */
object BuiltInCatalog {
    // 로직에서 참조하는 특수 키
    const val SCHEDULE = "SCHEDULE"
    const val MEMORY = "MEMORY" // 항상 활성인 catch-all

    /** 기본 활성 7종. */
    val defaults: List<Category> = listOf(
        Category(SCHEDULE, "일정", "🗓️", "특정 날짜/시간의 약속·행사·예정", CaptureType.SCHEDULE, true, true, 0),
        Category("STUDY", "공부", "📚", "학습·시험·과제·강의·학사(등록금 등)", CaptureType.STUDY, true, true, 1),
        Category("CHAT", "대화", "💬", "카톡 등 메신저 대화 캡처", CaptureType.CHAT, true, true, 2),
        Category("LINK", "링크·정보", "🔗", "유튜브·인스타·웹기사 등 콘텐츠/링크/영상", CaptureType.LINK, true, true, 3),
        Category("PLACE", "장소", "📍", "지도·길찾기·맛집·가게 정보", CaptureType.PLACE, true, true, 4),
        Category("SHOPPING", "쇼핑", "🛍️", "주문·결제·상품·영화/공연 예매·항공/숙소 예약", CaptureType.SHOPPING, true, true, 5),
        Category(MEMORY, "추억", "🖼️", "위에 안 맞는 사진·기타", CaptureType.MEMORY, true, true, 6),
    )

    /** 후보(기본 OFF). 사용자가 자주 쓰면 켜서 활성 집합에 넣는다. */
    val candidates: List<Category> = listOf(
        Category("RECIPE", "레시피", "🍳", "요리·레시피·음식 만드는 법", CaptureType.LINK, true, false, 7),
        Category("FASHION", "패션", "👗", "옷·코디·패션 아이템", CaptureType.SHOPPING, true, false, 8),
        Category("TRAVEL", "여행", "✈️", "여행 계획·명소·항공·숙소", CaptureType.PLACE, true, false, 9),
        Category("WORKOUT", "운동·건강", "🏋️", "운동·헬스·홈트·식단·건강", CaptureType.STUDY, true, false, 10),
        Category("MEME", "밈·짤", "😂", "웃긴 짤·밈·유머 이미지", CaptureType.MEMORY, true, false, 11),
        Category("MONEY", "가계부·금융", "💰", "가계부·소비 내역·주식·금융", CaptureType.SHOPPING, true, false, 12),
        Category("QUOTE", "글귀·명언", "✍️", "명언·좋은 글·문구", CaptureType.MEMORY, true, false, 13),
        Category("NEWS", "뉴스", "📰", "뉴스·기사·시사", CaptureType.LINK, true, false, 14),
        Category("BEAUTY", "뷰티", "💄", "화장품·뷰티·헤어·네일", CaptureType.SHOPPING, true, false, 15),
        Category("PET", "반려동물", "🐾", "강아지·고양이 등 반려동물", CaptureType.MEMORY, true, false, 16),
    )

    /** 최초 시드 전체(기본값+후보). */
    val all: List<Category> get() = defaults + candidates

    /** 커스텀 카테고리 기본 기반 종류(정보수집형으로 성장에 기여). */
    val CUSTOM_BASE = CaptureType.LINK
}
