package com.example.petling.domain

import com.example.petling.domain.engine.BranchResolver
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.ScheduleCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class BranchResolverTest {

    @Test
    fun empty_defaults_to_balanced() {
        assertEquals(Branch.BALANCED, BranchResolver.resolve(emptyMap()))
    }

    @Test
    fun study_majority_yields_study() {
        val map = mapOf(
            ScheduleCategory.STUDY to 6,
            ScheduleCategory.HOBBY to 2,
            ScheduleCategory.REST to 2,
        )
        assertEquals(Branch.STUDY, BranchResolver.resolve(map))
    }

    @Test
    fun study_exactly_half_counts_as_study() {
        val map = mapOf(
            ScheduleCategory.STUDY to 5,
            ScheduleCategory.APPOINTMENT to 5,
        )
        assertEquals(Branch.STUDY, BranchResolver.resolve(map))
    }

    @Test
    fun hobby_and_rest_combine_for_hobby_branch() {
        val map = mapOf(
            ScheduleCategory.HOBBY to 3,
            ScheduleCategory.REST to 3,
            ScheduleCategory.STUDY to 2,
            ScheduleCategory.APPOINTMENT to 2,
        )
        assertEquals(Branch.HOBBY, BranchResolver.resolve(map))
    }

    @Test
    fun uncategorized_majority_yields_balanced() {
        val map = mapOf(
            null to 8,
            ScheduleCategory.STUDY to 1,
            ScheduleCategory.HOBBY to 1,
        )
        assertEquals(Branch.BALANCED, BranchResolver.resolve(map))
    }

    @Test
    fun mixed_without_majority_yields_balanced() {
        val map = mapOf(
            ScheduleCategory.STUDY to 3,
            ScheduleCategory.APPOINTMENT to 4,
            ScheduleCategory.HOBBY to 2,
        )
        // study 3/9 < 50%, hobby+rest 2/9 < 50% -> balanced
        assertEquals(Branch.BALANCED, BranchResolver.resolve(map))
    }
}
