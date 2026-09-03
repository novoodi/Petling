package com.example.petling.ui.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.data.local.entity.MarketMedianEntity
import com.example.petling.data.local.entity.MarketProductEntity
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.market.MarketRepository
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.price.formatMarketDay
import com.example.petling.ui.price.won
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Neutral400
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MarketProductViewModel(
    private val market: MarketRepository,
    private val goodId: Long,
) : ViewModel() {

    val product: StateFlow<MarketProductEntity?> =
        flow { emit(market.product(goodId)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val medians: StateFlow<List<MarketMedianEntity>> =
        market.observeMedians(goodId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 이 참가격 상품에 매핑된 내 기록(가격표 촬영분). */
    val myEntries: StateFlow<List<PriceEntryEntity>> =
        market.observeMyEntries(goodId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** 참가격 상품 하나: 최신 조사 업태별 중앙값 + 조사일별 추이 차트(+내 기록 점). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketProductScreen(goodId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: MarketProductViewModel = viewModel(
        factory = viewModelFactory { initializer { MarketProductViewModel(container.marketRepository, goodId) } },
    )
    LaunchedEffect(Unit) { container.analytics.screen("market_product") }
    val product by vm.product.collectAsStateWithLifecycle()
    val medians by vm.medians.collectAsStateWithLifecycle()
    val myEntries by vm.myEntries.collectAsStateWithLifecycle()

    val days = medians.map { it.day }.distinct().sorted()
    val latestDay = days.lastOrNull()
    val latest = medians.filter { it.day == latestDay }.associate { it.type to it.priceWon }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(product?.name ?: "시장 가격") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
            },
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    latestDay?.let { "최신 조사 ${formatMarketDay(it)} · 업태별 중앙값" } ?: "조사 가격이 아직 없어요",
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                listOf("LM", "SM", "DP", "CS", "ALL").forEach { t ->
                    latest[t]?.let { price ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(MarketRepository.TYPE_LABELS[t] ?: t, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                won(price),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (t == "ALL") FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Text("가격 추이 · 조사 ${days.size}회분", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (days.size < 2) {
                    Text(
                        "조사 데이터가 쌓이면(격주) 선으로 이어져요. 지금은 최신 조사 한 점만 보여요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
                fun seriesOf(type: String, label: String, color: androidx.compose.ui.graphics.Color): TrendSeries? {
                    val pts = medians.filter { it.type == type }.mapNotNull { m ->
                        runCatching { LocalDate.parse(m.day, fmt).toEpochDay() }.getOrNull()?.let { TrendPoint(it, m.priceWon) }
                    }
                    return if (pts.isEmpty()) null else TrendSeries(label, color, pts)
                }
                val series = listOfNotNull(
                    seriesOf("LM", "대형마트 중앙값", Brand500),
                    seriesOf("ALL", "전체 중앙값", Neutral400),
                )
                val my = myEntries.map { TrendPoint(it.dateEpochDay, it.priceWon) }
                if (series.isNotEmpty() || my.isNotEmpty()) {
                    PriceTrendChart(series = series, myPoints = my, modifier = Modifier.fillMaxWidth())
                }
                if (my.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "내 기록 ${my.size}건이 점으로 표시돼요. 가격표를 찍어 이 상품으로 확인하면 여기에 쌓여요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    MarketRepository.SOURCE_LABEL,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
