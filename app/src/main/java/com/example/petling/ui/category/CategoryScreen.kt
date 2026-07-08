package com.example.petling.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.data.repository.CategoryRepository
import com.example.petling.domain.model.Category
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.library.categoryColors
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import com.example.petling.ui.theme.TextTertiary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(key: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(key, enabled) }
    }

    fun addCustom(label: String, emoji: String) {
        if (label.isBlank()) return
        viewModelScope.launch { repository.addCustom(label, emoji) }
    }

    fun deleteCustom(category: Category) {
        viewModelScope.launch { repository.deleteCustom(category) }
    }
}

/** 설정에서 여는 전체 화면. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CategoryManageScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: CategoryViewModel = viewModel(
        factory = viewModelFactory { initializer { CategoryViewModel(container.categoryRepository) } },
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 분류함") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPaddingFocused)
                .verticalScroll(rememberScrollState()),
        ) {
            CategoryManageBody(vm)
            Spacer(Modifier.height(Dimens.Space8))
        }
    }
}

/**
 * 카테고리 관리 본문(온보딩 단계·설정 화면 공용). 활성 토글 + 커스텀 추가/삭제.
 */
@Composable
fun CategoryManageBody(vm: CategoryViewModel) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Spacer(Modifier.height(Dimens.Space3))
    Text(
        "자주 모으는 분류만 켜두면 정리가 더 정확해져요. 5~8개를 추천해요.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextTertiary,
    )
    Spacer(Modifier.height(Dimens.Space3))

    categories.forEach { category ->
        CategoryRow(
            category = category,
            onToggle = { vm.setEnabled(category.key, it) },
            onDelete = { vm.deleteCustom(category) },
        )
        Spacer(Modifier.height(Dimens.Space2))
    }

    Spacer(Modifier.height(Dimens.Space2))
    PetlingButton("+ 직접 추가", onClick = { showAdd = true }, style = PetlingButtonStyle.Secondary)

    if (showAdd) {
        AddCategoryDialog(
            onAdd = { label, emoji -> vm.addCustom(label, emoji); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val (fg, bg) = categoryColors(category.baseType)
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category.emoji,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(0.dp))
                Text(
                    "  ${category.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = fg,
                )
                if (!category.isBuiltIn) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Close, contentDescription = "삭제", tint = TextTertiary)
                    }
                }
            }
            Switch(checked = category.enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏷️") }
    val emojiChoices = listOf("🏷️", "🍳", "👗", "✈️", "🏋️", "😂", "💰", "✍️", "📰", "💄", "🐾", "🎮", "🎵", "📷", "🎨")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 분류 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(10) },
                    label = { Text("이름 (예: 레시피)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.Space3))
                Text("아이콘", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(Dimens.Space2))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
                ) {
                    emojiChoices.forEach { e ->
                        Text(
                            e,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (e == emoji) MaterialTheme.colorScheme.primaryContainer else SurfaceSubtle)
                                .clickable { emoji = e }
                                .padding(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(label, emoji) }, enabled = label.isNotBlank()) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
