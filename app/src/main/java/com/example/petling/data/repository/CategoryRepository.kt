package com.example.petling.data.repository

import com.example.petling.data.local.dao.CategoryDao
import com.example.petling.data.local.entity.toDomain
import com.example.petling.data.local.entity.toEntity
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 사용자 맞춤 분류 카테고리 관리. 최초 실행 시 빌트인 카탈로그를 시드하고,
 * 활성 집합 관찰·토글·커스텀 추가/삭제·순서 변경을 제공한다.
 */
class CategoryRepository(
    private val dao: CategoryDao,
) {
    val allCategories: Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    val enabledCategories: Flow<List<Category>> =
        dao.observeEnabled().map { list -> list.map { it.toDomain() } }

    /** 최초 1회: 카탈로그 시드(이미 있으면 무시). */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(BuiltInCatalog.all.map { it.toEntity() })
        }
    }

    /** 분류에 사용할 활성 카테고리(항상 catch-all 보장). */
    suspend fun enabledForClassify(): List<Category> {
        val enabled = dao.getEnabled().map { it.toDomain() }
        if (enabled.any { it.key == BuiltInCatalog.MEMORY }) return enabled
        // catch-all이 꺼져 있으면 분류 안전을 위해 추가(비저장, 이번 분류용)
        val memory = BuiltInCatalog.defaults.first { it.key == BuiltInCatalog.MEMORY }
        return enabled + memory
    }

    /** key→Category 전체 매핑(표시·분기 환산용). */
    suspend fun mapByKey(): Map<String, Category> =
        dao.getAll().associate { it.key to it.toDomain() }

    suspend fun setEnabled(key: String, enabled: Boolean) {
        // catch-all(추억)은 항상 켜둔다.
        if (key == BuiltInCatalog.MEMORY && !enabled) return
        dao.setEnabled(key, enabled)
    }

    /** 커스텀 카테고리 추가. key는 순서 기반으로 생성. */
    suspend fun addCustom(label: String, emoji: String) {
        val order = (dao.maxSortOrder() ?: 0) + 1
        val key = "c_$order"
        dao.upsert(
            Category(
                key = key,
                label = label.trim().ifBlank { "새 분류" },
                emoji = emoji.ifBlank { "🏷️" },
                description = label.trim(),
                baseType = BuiltInCatalog.CUSTOM_BASE,
                isBuiltIn = false,
                enabled = true,
                sortOrder = order,
            ).toEntity(),
        )
    }

    /** 커스텀 카테고리 삭제(빌트인은 삭제 대신 토글로 끔). */
    suspend fun deleteCustom(category: Category) {
        if (category.isBuiltIn) return
        dao.delete(category.toEntity())
    }

    /** 개발/초기화 후 재시드. */
    suspend fun reseed() {
        dao.insertAll(BuiltInCatalog.all.map { it.toEntity() })
    }
}
