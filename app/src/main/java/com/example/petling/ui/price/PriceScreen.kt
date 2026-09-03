package com.example.petling.ui.price

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.data.market.MarketInsight
import com.example.petling.data.market.MarketOverview
import com.example.petling.data.market.MarketRepository
import com.example.petling.data.price.NanoState
import com.example.petling.data.repository.TrackedProduct
import com.example.petling.domain.price.NanoNameGuard
import com.example.petling.ui.ActionIntents
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.OnDeviceAiIntroDialog
import com.example.petling.ui.components.PetlingCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 원화 표기. */
internal fun won(value: Int): String = "%,d원".format(value)

private const val PREFS_NAME = "martmemo"
private const val PREF_AI_INTRO_SEEN = "ai_intro_seen"
/** AICore 상태 확인을 이만큼 기다린 뒤엔 확인 중 문구로라도 안내를 띄운다. */
private const val INTRO_WAIT_MS = 4_000L

internal fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("M/d"))

/** 참가격 조사일 "20260828" → "8/28". 형식이 다르면 그대로. */
internal fun formatMarketDay(day: String): String = runCatching {
    LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyyMMdd")).format(DateTimeFormatter.ofPattern("M/d"))
}.getOrDefault(day)

/**
 * 가격 추적 탭: 마트 가격표를 찍으면 상품·가격을 읽어
 * 네이버 최저가·지난 방문 가격과 비교해 기록한다.
 */
@Composable
fun PriceScreen(onOpenProduct: (Long) -> Unit, onOpenMarket: () -> Unit, onOpenMarketProduct: (Long) -> Unit) {
    val container = appContainer()
    val vm: PriceViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PriceViewModel(container.priceRepository, container.marketRepository, container.analytics) }
        },
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) { container.analytics.screen("price_home") }
    val tracked by vm.tracked.collectAsStateWithLifecycle()
    val overview by vm.overview.collectAsStateWithLifecycle()
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    // 첫 실행 안내: 이 폰이 AI 지원인지 / 아니면 어떻게 동작하는지 먼저 알려준다(1회).
    // AICore 응답이 늦으면 기다리다가 일정 시간 후 "확인 중" 문구로라도 띄운다.
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showIntro by remember { mutableStateOf(!prefs.getBoolean(PREF_AI_INTRO_SEEN, false)) }
    val nanoState by container.priceTagExtractor.state.collectAsStateWithLifecycle()
    var introWaitOver by remember { mutableStateOf(false) }
    LaunchedEffect(showIntro) {
        if (showIntro) {
            delay(INTRO_WAIT_MS)
            introWaitOver = true
        }
    }
    if (showIntro && (nanoState !is NanoState.Checking || introWaitOver)) {
        OnDeviceAiIntroDialog(
            state = nanoState,
            onDownload = { container.priceTagExtractor.download() },
            onDismiss = {
                prefs.edit().putBoolean(PREF_AI_INTRO_SEEN, true).apply()
                showIntro = false
            },
        )
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        message?.let {
            scope.launch { snackbar.showSnackbar(it) }
            vm.consumeMessage()
        }
    }

    // 카메라 촬영: 캐시에 임시 파일 → FileProvider Uri → 성공 시 분석
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri.value
        if (ok && uri != null) vm.analyze(uri)
    }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyze(uri, source = "album")
    }

    fun launchCamera() {
        runCatching {
            val dir = File(context.cacheDir, "price").apply { mkdirs() }
            val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val a = analysis) {
            null -> TrackedList(
                tracked = tracked,
                overview = overview,
                onOpenMarket = onOpenMarket,
                onOpenMarketProduct = {
                    container.analytics.marketProductOpened("home")
                    onOpenMarketProduct(it)
                },
                onCamera = { launchCamera() },
                onPick = {
                    pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onOpenProduct = onOpenProduct,
            )
            AnalysisUi.Loading -> AnalyzingBody()
            is AnalysisUi.Ready -> ResultBody(
                ui = a,
                onNameChange = vm::updateName,
                onPriceChange = vm::updatePriceText,
                onPickCandidate = vm::pickCandidate,
                onStoreChange = vm::updateStore,
                onPickMarket = vm::pickMarketProduct,
                onRequery = vm::requery,
                onSave = vm::save,
                onCancel = vm::dismissAnalysis,
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun TrackedList(
    tracked: List<TrackedProduct>,
    overview: MarketOverview?,
    onOpenMarket: () -> Unit,
    onOpenMarketProduct: (Long) -> Unit,
    onCamera: () -> Unit,
    onPick: () -> Unit,
    onOpenProduct: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("가격 추적", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "마트 가격표를 찍으면 지난 방문 때 가격과 비교하고\n온라인 시세도 한 번에 확인할 수 있어요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCamera, modifier = Modifier.weight(1f)) {
                    Text("📷 가격표 찍기")
                }
                FilledTonalButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                    Text("앨범에서 선택")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        // 기록이 0건이어도 볼 것: 이번 조사 시장 물가(참가격). 촬영 없이 열자마자 답이 있게.
        overview?.let { ov ->
            item { MarketOverviewCard(ov, onOpenMarket = onOpenMarket, onOpenProduct = onOpenMarketProduct) }
        }
        if (tracked.isEmpty()) {
            item {
                PetlingCard(modifier = Modifier.fillMaxWidth()) {
                    Text("아직 기록한 상품이 없어요", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "마트에서 자주 사는 물건의 가격표를 찍어보세요.\n다음에 또 찍으면 그때랑 얼마 차이나는지 알려드려요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(tracked, key = { it.product.id }) { item ->
            TrackedProductCard(item, onClick = { onOpenProduct(item.product.id) })
        }
    }
}

/** 홈 상단 "이번 주 시장 물가": 대표 생필품 5개 + 직전 조사 대비 변동. */
@Composable
private fun MarketOverviewCard(ov: MarketOverview, onOpenMarket: () -> Unit, onOpenProduct: (Long) -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏷 이번 조사 시장 물가", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${formatMarketDay(ov.latestDay)} 조사",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        ov.staples.take(5).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenProduct(item.product.id) }.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(won(item.change.nowWon), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                ChangeBadge(item.change.changePct)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (ov.previousDay == null) "전국 중앙값 · 다음 조사부터 오르내림이 표시돼요 · 시세 더 보기 ›"
            else "전국 중앙값 · ${formatMarketDay(ov.previousDay)} 조사 대비 · 시세 더 보기 ›",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onOpenMarket),
        )
    }
}

/** ▲3% / ▼2% / – 배지. 직전 조사가 없으면 빈칸. */
@Composable
internal fun ChangeBadge(pct: Double?) {
    val (text, color) = when {
        pct == null -> "" to MaterialTheme.colorScheme.onSurfaceVariant
        pct >= 1.0 -> "▲${pct.toInt()}%" to MaterialTheme.colorScheme.error
        pct <= -1.0 -> "▼${(-pct).toInt()}%" to MaterialTheme.colorScheme.primary
        else -> "–" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.width(44.dp))
}

@Composable
private fun TrackedProductCard(item: TrackedProduct, onClick: () -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    // 이름에 이미 "840g"이 들어 있으면 용량을 덧붙이지 않는다(QA: "…840g 840g")
                    val volume = item.product.volumeAmount?.takeIf { !NanoNameGuard.containsVolume(item.product.name) }?.let { amt ->
                        val text = if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
                        " $text${item.product.volumeUnit.orEmpty()}"
                    }.orEmpty()
                    Text(
                        item.product.name + volume,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val latest = item.latest
                    if (latest != null) {
                        Text(
                            listOfNotNull(
                                formatEpochDay(latest.dateEpochDay),
                                latest.storeName,
                                "기록 ${item.entryCount}회",
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    item.latest?.let { latest ->
                        Text(
                            won(latest.priceWon),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item.deltaWon?.let { delta ->
                        val (text, color) = when {
                            delta > 0 -> "▲ ${won(delta)}" to MaterialTheme.colorScheme.error
                            delta < 0 -> "▼ ${won(-delta)}" to MaterialTheme.colorScheme.primary
                            else -> "변동 없음" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
                        if (item.deltaOtherStore) {
                            Text(
                                "다른 매장 기준",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item.latest?.naverPriceWon?.let { naver ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "기록 당시 네이버 최저가 ${won(naver)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

/**
 * 시장 가격 카드. 확정(자동 또는 사용자 선택)이면 업태별 중앙값과 현재가 비교,
 * 미확정이면 후보 칩 "이 상품이 맞나요?"를 보여준다.
 */
@Composable
private fun MarketCard(market: MarketInsight, priceNow: Int?, onPick: (Long?) -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("🏷 시장 가격 · ${formatMarketDay(market.latestDay)} 조사", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (market.confident) {
                Text(market.product.name, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                val line = listOf("LM", "SM", "DP", "CS").mapNotNull { t ->
                    market.medianByType[t]?.let { "${MarketRepository.TYPE_LABELS[t]} ${won(it)}" }
                }.joinToString(" · ")
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val ref = market.reference
                if (priceNow != null && ref != null) {
                    val (type, median) = ref
                    val label = MarketRepository.TYPE_LABELS[type] ?: type
                    val pct = ((priceNow - median) * 100.0 / median).toInt()
                    val verdict = when {
                        pct <= -3 -> "$label 중앙값보다 ${-pct}% 저렴해요 🎉"
                        pct >= 3 -> "$label 중앙값보다 $pct% 비싸요"
                        else -> "$label 중앙값과 비슷해요"
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(verdict, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "다른 상품이에요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onPick(null) },
                )
            } else {
                Text(
                    "참가격에 비슷한 상품이 있어요 — 이 상품이 맞나요?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    market.candidates.forEach { c ->
                        SuggestionChip(onClick = { onPick(c.product.id) }, label = { Text(c.product.name) })
                    }
                    SuggestionChip(onClick = { onPick(null) }, label = { Text("해당 없음") })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                MarketRepository.SOURCE_LABEL,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 매장 선택: 최근 매장 칩 + 직접 입력(참가격 판매점 자동완성). 기록이 없는 첫 사용은 입력창부터 보여준다. */
@Composable
private fun StoreSection(
    storeName: String,
    recentStores: List<String>,
    suggestions: List<String>,
    onStoreChange: (String) -> Unit,
) {
    val isCustom = storeName.isNotBlank() && storeName !in recentStores
    var typing by remember(recentStores) { mutableStateOf(isCustom || recentStores.isEmpty()) }
    val showField = typing || isCustom

    Column {
        Text("매장", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            recentStores.forEach { store ->
                FilterChip(
                    selected = !showField && storeName == store,
                    onClick = {
                        typing = false
                        onStoreChange(if (storeName == store) "" else store)
                    },
                    label = { Text(store) },
                )
            }
            FilterChip(
                selected = showField,
                onClick = {
                    typing = true
                    if (!isCustom) onStoreChange("")
                },
                label = { Text(if (recentStores.isEmpty()) "매장 입력" else "직접 입력") },
            )
        }
        if (showField) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreChange,
                label = { Text("매장 이름") },
                placeholder = { Text("예: 이마트 성수점") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { s ->
                        SuggestionChip(onClick = { onStoreChange(s) }, label = { Text(s) })
                    }
                }
            }
        }
        if (storeName.isBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "매장을 고르면 같은 매장끼리 가격을 비교해요 (선택 사항)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnalyzingBody() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("가격표를 읽고 있어요…", style = MaterialTheme.typography.titleMedium)
        Text(
            "상품명과 가격을 찾고 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultBody(
    ui: AnalysisUi.Ready,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPickCandidate: (Int) -> Unit,
    onStoreChange: (String) -> Unit,
    onPickMarket: (Long?) -> Unit,
    onRequery: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val analysis = ui.analysis
    val tag = analysis.tag
    val priceNow = ui.priceText.toIntOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("가격표 확인", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = ui.name,
            onValueChange = onNameChange,
            label = { Text("상품명") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("이름이 다르면 고친 뒤 '다시 확인'을 눌러주세요")
            },
        )
        OutlinedButton(onClick = onRequery, enabled = !ui.requerying && ui.name.isNotBlank()) {
            Text(if (ui.requerying) "확인 중…" else "🔍 이 이름으로 다시 확인")
        }

        OutlinedTextField(
            value = ui.priceText,
            onValueChange = onPriceChange,
            label = { Text("가격") },
            suffix = { Text("원") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        // 규칙이 최종가를 확정하지 못한 경우: 후보 칩에서 탭 한 번으로 선택
        if (priceNow == null && tag.priceCandidatesWon.size > 1) {
            Text(
                "사진에서 읽힌 가격 후보예요 — 맞는 것을 골라주세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tag.priceCandidatesWon.take(4).forEach { candidate ->
                    SuggestionChip(onClick = { onPickCandidate(candidate) }, label = { Text(won(candidate)) })
                }
            }
        }
        if (tag.originalPriceWon != null && priceNow != null && tag.originalPriceWon > priceNow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    won(tag.originalPriceWon),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${won(tag.originalPriceWon - priceNow)} 할인 중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        tag.saleEndEpochDay?.let {
            Text(
                "🏷️ 행사가 · ${formatEpochDay(it)}까지",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 매장: 같은 상품도 매장마다 가격이 달라 비교의 축이 된다(위치 권한 없이 수동 선택)
        StoreSection(
            storeName = ui.storeName,
            recentStores = ui.recentStores,
            suggestions = ui.storeSuggestions,
            onStoreChange = onStoreChange,
        )

        // 재방문: 지난 기록과 비교(같은 매장 기록 우선)
        analysis.previous?.let { previous ->
            val prev = previous.entry
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("🔁 전에 기록한 상품이에요", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            "${formatEpochDay(prev.dateEpochDay)}에 ${won(prev.priceWon)}이었어요",
                            prev.storeName,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!previous.sameStore) {
                        Text(
                            prev.storeName?.let { "이 매장 기록이 없어 $it 가격과 비교해요" }
                                ?: "매장 정보가 없는 기록과 비교해요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (priceNow != null) {
                        val delta = priceNow - prev.priceWon
                        val verdict = when {
                            delta < 0 -> "지난번보다 ${won(-delta)} 저렴해요 🎉"
                            delta > 0 -> "지난번보다 ${won(delta)} 올랐어요 📈"
                            else -> "지난번과 같은 가격이에요"
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(verdict, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 시장 가격(참가격): 첫 촬영에도 "지금 가격이 시장 대비 어떤지"를 보여준다
        analysis.market?.let { market ->
            MarketCard(market = market, priceNow = priceNow, onPick = onPickMarket)
        }

        // 온라인 시세: 공식 가격 API가 없어(네이버 쇼핑 검색 API 2026-07 종료)
        // 상품명이 채워진 검색 딥링크로 원탭 확인만 제공한다.
        if (ui.name.isNotBlank()) {
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("🌐 온라인 시세 확인", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "상품명이 검색된 상태로 바로 열려요 · 온라인가는 배송비 제외일 수 있어요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                ActionIntents.openUrl(
                                    context,
                                    "https://search.shopping.naver.com/search/all?query=${Uri.encode(ui.name)}",
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("네이버쇼핑")
                        }
                        OutlinedButton(
                            onClick = {
                                ActionIntents.openUrl(
                                    context,
                                    "https://www.coupang.com/np/search?q=${Uri.encode(ui.name)}",
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("쿠팡")
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("취소")
            }
            Button(
                onClick = onSave,
                enabled = ui.name.isNotBlank() && (priceNow ?: 0) > 0,
                modifier = Modifier.weight(2f),
            ) {
                Text("이 가격으로 기록하기")
            }
        }
    }
}
