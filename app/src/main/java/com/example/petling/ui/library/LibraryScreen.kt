package com.example.petling.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.CaptureItem
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CaptureType
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.overlay.perch
import com.example.petling.ui.theme.Brand50
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import com.example.petling.ui.theme.TextTertiary

@Composable
fun LibraryScreen(onOpenCapture: (Long) -> Unit) {
    val container = appContainer()
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LibraryViewModel(container.captureRepository, container.categoryRepository) }
        },
    )
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.keyFilter.collectAsStateWithLifecycle()
    val items by vm.visibleItems.collectAsStateWithLifecycle()
    val filterCategories by vm.filterCategories.collectAsStateWithLifecycle()
    val categoriesByKey by vm.categoriesByKey.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space4))
        Text("보관함", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Dimens.Space3))
        OutlinedTextField(
            value = query,
            onValueChange = vm::updateQuery,
            placeholder = { Text("캡처 내용 검색") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.Space3))
        FilterChips(categories = filterCategories, selected = filter, onSelect = vm::setKeyFilter)
        Spacer(Modifier.height(Dimens.Space3))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isNotBlank()) "검색 결과가 없어요." else "아직 보관한 캡처가 없어요.\n스크린샷을 공유해보세요.",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
                contentPadding = PaddingValues(bottom = Dimens.Space8),
            ) {
                items(items, key = { it.id }) { item ->
                    CaptureListCard(item, categoriesByKey[item.categoryKey]) { onOpenCapture(item.id) }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(categories: List<Category>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
        item {
            Chip("전체", selected == null) { onSelect(null) }
        }
        items(categories, key = { it.key }) { category ->
            Chip(category.display, selected == category.key) { onSelect(category.key) }
        }
    }
}

@Composable
private fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isSelected) Brand50 else SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2),
    )
}

/**
 * 정보 카드: 추출된 내용(제목·OCR)과 비서가 한 행동(일정/링크/지도)을 앞세우고,
 * 이미지는 작은 근거 썸네일로 곁들인다. "사진 정리함"이 아니라 "내용 정리함"으로 읽히게.
 */
@Composable
private fun CaptureListCard(item: CaptureItem, category: Category?, onClick: () -> Unit) {
    PetlingCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .clickable(onClick = onClick)
            .perch("lib-${item.id}"), // 캐릭터가 올라앉을 수 있는 자리
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CaptureThumbnail(
                item.imagePath,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusMd)),
            )
            Spacer(Modifier.size(Dimens.Space3))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category)
                    ActionTags(item, category)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val info = infoLine(item)
                if (info.isNotBlank()) {
                    Text(
                        info,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = TextTertiary)
        }
    }
}

/** 비서가 한 행동을 작은 태그로. */
@Composable
private fun ActionTags(item: CaptureItem, category: Category?) {
    val tags = buildList {
        if (item.sourceScheduleId != null) add("🗓️ 일정")
        if (item.linkUrl != null) add("🔗 링크")
        if (category?.baseType == CaptureType.PLACE) add("📍 지도")
    }
    tags.forEach { tag ->
        Spacer(Modifier.size(4.dp))
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Brand50)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** 이미지 대신 앞세울 '내용' 한 줄: AI 요약 → 링크 도메인 → OCR 앞부분. */
private fun infoLine(item: CaptureItem): String {
    item.summary?.takeIf { it.isNotBlank() }?.let { return it }
    item.linkUrl?.let { return domainOf(it) }
    return item.ocrText
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(" ")
        .take(80)
}

private fun domainOf(url: String): String =
    url.substringAfter("://").substringBefore("/").removePrefix("www.").ifBlank { url }
