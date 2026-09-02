package com.example.petling.domain.price

/**
 * 가격표 사진에서 추출한 정보. 모든 필드는 사용자가 확인 화면에서 수정할 수 있는 초안이다.
 * 숫자 값은 OCR 텍스트에서 규칙으로 뽑는다 — Nano의 산술/숫자 생성은 신뢰하지 않는다(QA 교훈).
 */
data class PriceTagInfo(
    /** 상품명(용량 표기 제거). 예: "존쿡 델리미트 캠핑파티" */
    val name: String,
    /** 최종 판매가(할인 반영). 원 단위. */
    val priceWon: Int?,
    /** 할인 전 정가(할인 표기가 있을 때만). */
    val originalPriceWon: Int? = null,
    /** 용량 수치. 예: 840.0 */
    val volumeAmount: Double? = null,
    /** 용량 단위(g/kg/ml/L 등 소문자 정규화). */
    val volumeUnit: String? = null,
    /** 단위가 기준량. 예: "10g당 176원"의 10.0 */
    val unitBaseAmount: Double? = null,
    val unitBaseUnit: String? = null,
    /** 단위가. 예: 176 */
    val unitPriceWon: Int? = null,
    /** 행사기간 종료일(epochDay). 없으면 null. */
    val saleEndEpochDay: Long? = null,
    /** 가격표 바코드(13자리). 재방문 매칭의 최우선 키. */
    val barcode: String? = null,
    /** 규칙이 확신하지 못한 가격 후보들(Nano/사용자 선택용, 내림차순). */
    val priceCandidatesWon: List<Int> = emptyList(),
) {
    val isEmpty: Boolean get() = name.isBlank() && priceWon == null
}

/** 이름 매칭용 정규화: 공백/괄호/특수문자 제거. */
fun normalizeProductName(name: String): String =
    name.filter { it.isLetterOrDigit() }.lowercase()
