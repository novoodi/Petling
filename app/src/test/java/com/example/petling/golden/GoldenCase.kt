package com.example.petling.golden

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 골든 데이터셋 케이스: "OCR 텍스트 입력 → 기대 파싱 결과" 쌍.
 * 실사용에서 오파싱을 발견하면 케이스로 박제해 회귀를 막는다.
 * 자세한 추가 방법은 src/test/resources/golden/README.md 참조.
 */
@Serializable
data class GoldenCase(
    val id: String,
    /** 의도 라벨: chat|timetable|shopping|place|link|memory|plain */
    val intent: String,
    /** OCR 결과 시뮬레이션 텍스트 */
    val text: String,
    /** 상대 날짜 해석 기준일 (ISO) */
    val today: String,
    /** 기대 일정 목록. 빈 배열 = 일정 없음이 정답 */
    val expected: List<GoldenSchedule> = emptyList(),
    val note: String = "",
)

@Serializable
data class GoldenSchedule(
    /** 부분 일치로 채점(정규화 후 포함 관계) */
    val title: String? = null,
    /** ISO 날짜 */
    val date: String? = null,
    /** "HH:mm", null = 종일 */
    val time: String? = null,
    val location: String? = null,
)

@Serializable
data class GoldenBaseline(
    val version: Int = 1,
    val parserTotal: Double = 0.0,
    val perCase: Map<String, Double> = emptyMap(),
)

object GoldenDataset {
    private val json = Json { ignoreUnknownKeys = true }

    val FILES = listOf("chat", "timetable", "shopping", "place", "link", "watermark", "plain")

    fun load(): List<GoldenCase> = FILES.flatMap { name ->
        json.decodeFromString<List<GoldenCase>>(readResource("golden/cases/$name.json"))
    }

    fun baseline(): GoldenBaseline =
        json.decodeFromString(readResource("golden/baseline.json"))

    private fun readResource(path: String): String {
        val stream = requireNotNull(GoldenDataset::class.java.classLoader.getResourceAsStream(path)) {
            "테스트 리소스를 찾을 수 없음: $path"
        }
        return stream.bufferedReader().readText()
    }
}
