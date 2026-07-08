package com.example.petling.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.CaptureRepository
import com.example.petling.data.repository.CategoryRepository
import com.example.petling.domain.model.CaptureItem
import com.example.petling.domain.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: CaptureRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _keyFilter = MutableStateFlow<String?>(null)
    val keyFilter: StateFlow<String?> = _keyFilter.asStateFlow()

    /** 필터 칩용 활성 카테고리. */
    val filterCategories: StateFlow<List<Category>> =
        categoryRepository.enabledCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 배지 표시용 key→Category(비활성 포함, 삭제된 커스텀은 미포함). */
    val categoriesByKey: StateFlow<Map<String, Category>> =
        categoryRepository.allCategories
            .map { list -> list.associateBy { it.key } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val items: StateFlow<List<CaptureItem>> =
        combine(_query, _keyFilter) { q, filter -> q to filter }
            .flatMapLatest { (q, filter) ->
                when {
                    q.isNotBlank() -> repository.search(q)
                    filter != null -> repository.observeByKey(filter)
                    else -> repository.observeAll()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 검색 중일 때 타입 필터도 함께 적용(검색은 전체 대상이므로 클라이언트에서 필터). */
    val visibleItems: StateFlow<List<CaptureItem>> =
        combine(items, _keyFilter, _query) { list, filter, q ->
            if (q.isNotBlank() && filter != null) list.filter { it.categoryKey == filter } else list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(q: String) { _query.value = q }
    fun setKeyFilter(key: String?) { _keyFilter.value = key }
}
