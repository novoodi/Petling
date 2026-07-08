package com.example.petling.domain.capture

import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CaptureType

/**
 * 지배적인 캡처 종류(기반 종류 기준)로 진화 갈래를 정한다. 커스텀 카테고리도 baseType으로 환산.
 * - 공부형(STUDY): 공부 자료를 많이 모음
 * - 정보수집형(HOBBY): 링크·장소·쇼핑(및 그 기반 커스텀)을 많이 모음
 * - 균형형(BALANCED): 대화·추억·일정 위주 또는 골고루
 */
object CaptureBranchResolver {

    /**
     * @param countByKey 카테고리 key별 캡처 수.
     * @param categories key→baseType 환산용(전체 카테고리; 활성/비활성 무관).
     */
    fun resolve(countByKey: Map<String, Int>, categories: List<Category>): Branch {
        val byKey = categories.associateBy { it.key }
        var total = 0
        var study = 0
        var collector = 0
        for ((key, cnt) in countByKey) {
            total += cnt
            when (byKey[key]?.baseType) {
                CaptureType.STUDY -> study += cnt
                CaptureType.LINK, CaptureType.PLACE, CaptureType.SHOPPING -> collector += cnt
                else -> Unit
            }
        }
        if (total == 0) return Branch.BALANCED

        return when {
            study * 2 >= total -> Branch.STUDY
            collector * 2 >= total -> Branch.HOBBY
            else -> Branch.BALANCED
        }
    }

    /** 진화 갈래의 정리함 맥락 표시명. */
    fun displayName(branch: Branch?): String = when (branch) {
        Branch.STUDY -> "공부형"
        Branch.HOBBY -> "정보수집형"
        Branch.BALANCED, null -> "균형형"
    }
}
