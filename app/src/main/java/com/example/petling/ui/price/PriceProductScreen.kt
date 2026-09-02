package com.example.petling.ui.price

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import com.example.petling.data.market.MarketRepository
import com.example.petling.data.repository.PriceRepository
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 상세 상단 한 줄: 참가격 상품명 + 최신 조사일 + 기준 업태 중앙값. */
data class MarketLine(val productName: String, val day: String, val typeLabel: String, val medianWon: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class PriceProductViewModel(
    private val repository: PriceRepository,
    private val market: MarketRepository,
    private val productId: Long,
) : ViewModel() {

    /** 저장된 참가격 매핑이 있을 때만 값이 있다. */
    val marketLine: StateFlow<MarketLine?> =
        repository.observeProduct(productId)
            .flatMapLatest { p ->
                val goodId = p?.marketGoodId ?: return@flatMapLatest flowOf(null)
                market.observeMedians(goodId).map { medians ->
                    val day = medians.maxOfOrNull { it.day } ?: return@map null
                    val latest = medians.filter { it.day == day }.associate { it.type to it.priceWon }
                    val type = if (latest.containsKey("LM")) "LM" else "ALL"
                    val median = latest[type] ?: return@map null
                    val name = market.product(goodId)?.name ?: return@map null
                    MarketLine(name, day, MarketRepository.TYPE_LABELS[type] ?: type, median)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val product: StateFlow<PriceProductEntity?> =
        repository.observeProduct(productId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val entries: StateFlow<List<PriceEntryEntity>> =
        repository.observeEntries(productId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            onDeleted()
        }
    }
}

/** 상품 하나의 가격 이력(방문할 때마다 한 줄씩 쌓인다). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceProductScreen(productId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: PriceProductViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PriceProductViewModel(container.priceRepository, container.marketRepository, productId) }
        },
    )
    val product by vm.product.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val marketLine by vm.marketLine.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(product?.name ?: "가격 이력") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            },
            actions = {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "삭제")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val p = product
                if (p != null) {
                    val volume = p.volumeAmount?.let { amt ->
                        val text = if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
                        "$text${p.volumeUnit.orEmpty()}"
                    }
                    Text(
                        listOfNotNull(volume, "기록 ${entries.size}회").joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    marketLine?.let { m ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "🏷 ${m.typeLabel} 시장 중앙값 ${won(m.medianWon)} · ${formatMarketDay(m.day)} 조사 · ${m.productName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            items(entries, key = { it.id }) { entry ->
                val index = entries.indexOf(entry)
                val previous = entries.getOrNull(index + 1)
                EntryCard(entry, previous)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("이 상품의 기록을 모두 삭제할까요?") },
            text = { Text("가격 이력과 사진이 함께 삭제되며 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onDeleted = onBack)
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun EntryCard(entry: PriceEntryEntity, previous: PriceEntryEntity?) {
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatEpochDay(entry.dateEpochDay),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                entry.storeName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entry.naverPriceWon?.let {
                    Text(
                        "당시 네이버 최저가 ${won(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    entry.originalPriceWon?.takeIf { it > entry.priceWon }?.let {
                        Text(
                            won(it),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        won(entry.priceWon),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                previous?.let { prev ->
                    val delta = entry.priceWon - prev.priceWon
                    val (text, color) = when {
                        delta > 0 -> "▲ ${won(delta)}" to MaterialTheme.colorScheme.error
                        delta < 0 -> "▼ ${won(-delta)}" to MaterialTheme.colorScheme.primary
                        else -> "변동 없음" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
                }
            }
        }
    }
}
