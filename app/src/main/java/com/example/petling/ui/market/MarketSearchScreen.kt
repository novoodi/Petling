package com.example.petling.ui.market

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.data.Analytics
import com.example.petling.data.market.MarketOverview
import com.example.petling.data.market.MarketOverviewItem
import com.example.petling.data.market.MarketRepository
import com.example.petling.data.market.MarketSearchItem
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.price.ChangeBadge
import com.example.petling.ui.price.formatMarketDay
import com.example.petling.ui.price.won
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MarketSearchViewModel(private val market: MarketRepository, private val analytics: Analytics) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val syncState = market.state

    val results: StateFlow<List<MarketSearchItem>> =
        _query.debounce(150)
            .mapLatest { q ->
                if (q.isBlank()) emptyList() else market.search(q).also { analytics.marketSearched(it.size) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _overview = MutableStateFlow<MarketOverview?>(null)
    val overview: StateFlow<MarketOverview?> = _overview.asStateFlow()

    init {
        viewModelScope.launch {
            market.state.collect { s -> if (s.hasData && !s.syncing) _overview.value = runCatching { market.overview(topN = 8) }.getOrNull() }
        }
    }

    fun setQuery(q: String) {
        _query.value = q.take(30)
    }
}

/** 시세 탭: 참가격 상품을 이름으로 찾아 시장 중앙값과 추이를 본다(촬영 없이). */
@Composable
fun MarketSearchScreen(onOpenProduct: (Long) -> Unit) {
    val container = appContainer()
    val vm: MarketSearchViewModel = viewModel(
        factory = viewModelFactory { initializer { MarketSearchViewModel(container.marketRepository, container.analytics) } },
    )
    LaunchedEffect(Unit) { container.analytics.screen("market_search") }
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val sync by vm.syncState.collectAsStateWithLifecycle()
    val overview by vm.overview.collectAsStateWithLifecycle()
    val open: (Long) -> Unit = { container.analytics.marketProductOpened("search"); onOpenProduct(it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("시세 검색", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (sync.hasData) {
                    "한국소비자원 참가격 ${sync.productCount}개 상품의 전국 시장 중앙값이에요 · 최신 조사 ${sync.latestDay?.let { formatMarketDay(it) } ?: "-"}"
                } else {
                    "시장 가격 데이터를 아직 받지 못했어요. Wi-Fi에서 설정 → 시장 가격 데이터 → 지금 갱신을 눌러주세요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("상품 이름 (예: 계란, 우유, 신라면)") },
            )
        }
        if (query.isBlank()) {
            item {
                Text(
                    "많이 찾는 상품",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("계란", "우유", "라면", "쌀", "휴지").forEach { s ->
                        SuggestionChip(onClick = { vm.setQuery(s) }, label = { Text(s) })
                    }
                }
            }
            // 검색 전에도 볼 것: 이번 조사에서 내린/오른 상품(조사 2회분 이상일 때), 없으면 대표 생필품
            overview?.let { ov ->
                if (ov.down.isNotEmpty() || ov.up.isNotEmpty()) {
                    item { MoversCard("📉 이번 조사에서 내린 상품", ov, ov.down, open) }
                    item { MoversCard("📈 이번 조사에서 오른 상품", ov, ov.up, open) }
                } else if (ov.staples.isNotEmpty()) {
                    item { MoversCard("🧺 대표 생필품 전국 중앙값", ov, ov.staples, open) }
                }
            }
        } else if (results.isEmpty()) {
            item {
                Text(
                    "‘$query’와 비슷한 상품이 참가격 목록에 없어요. 참가격은 대표 생필품 ${sync.productCount}개만 조사해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(results, key = { it.product.id }) { item ->
            PetlingCard(modifier = Modifier.fillMaxWidth().clickable { open(item.product.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                item.latestLM?.let { "대형마트 ${won(it)}" },
                                item.latestAll?.let { "전체 ${won(it)}" },
                            ).joinToString(" · ").ifBlank { "최신 조사 가격 없음" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 내린/오른/대표 상품 목록 카드. 한 줄 = 상품명 · 전체 중앙값 · 변동 배지. */
@Composable
private fun MoversCard(title: String, ov: MarketOverview, items: List<MarketOverviewItem>, onOpen: (Long) -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                ov.previousDay?.let { "${formatMarketDay(it)} → ${formatMarketDay(ov.latestDay)}" } ?: "${formatMarketDay(ov.latestDay)} 조사",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(item.product.id) }.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.product.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Text(won(item.change.nowWon), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                ChangeBadge(item.change.changePct)
            }
        }
    }
}
