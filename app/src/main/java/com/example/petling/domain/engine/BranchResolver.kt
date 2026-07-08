package com.example.petling.domain.engine

import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.ScheduleCategory

/**
 * 성장기 2 진입 시 진화 갈래 자동 판정.
 * 사용자가 선택하는 것이 아니라 "내 일정 패턴이 만든 모습"이라는 서사를 유지한다.
 */
object BranchResolver {

    fun resolve(completedByCategory: Map<out ScheduleCategory?, Int>): Branch {
        val total = completedByCategory.values.sum()
        if (total == 0) return Branch.BALANCED

        val study = completedByCategory[ScheduleCategory.STUDY] ?: 0
        val hobbyRest = (completedByCategory[ScheduleCategory.HOBBY] ?: 0) +
            (completedByCategory[ScheduleCategory.REST] ?: 0)

        return when {
            study * 2 >= total -> Branch.STUDY       // 학업 비중 50% 이상
            hobbyRest * 2 >= total -> Branch.HOBBY   // 취미+휴식 비중 50% 이상
            else -> Branch.BALANCED                  // 그 외(무카테고리 다수 포함)
        }
    }
}
