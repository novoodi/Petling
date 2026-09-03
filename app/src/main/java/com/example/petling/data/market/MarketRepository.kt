package com.example.petling.data.market

import android.content.Context
import androidx.room.withTransaction
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.local.dao.MarketDao
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.data.local.entity.MarketMedianEntity
import com.example.petling.data.local.entity.MarketProductEntity
import com.example.petling.data.local.entity.MarketStoreEntity
import com.example.petling.domain.AppClock
import com.example.petling.domain.market.MarketCandidate
import com.example.petling.domain.market.MarketMatch
import com.example.petling.domain.market.MarketMatcher
import com.example.petling.domain.price.normalizeStoreName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * 시장 가격(한국소비자원 참가격) 로컬 사본 관리.
 *
 * 앱은 공공 API를 직접 부르지 않는다 — GitHub Actions가 격주로 정리해 GitHub Pages에 게시한
 * JSON(index.json 200KB + trend.json 40KB)만 내려받아 Room에 넣는다. 사용자 사진·기록은 어떤 것도 올라가지 않는다.
 * 촬영·확인 화면은 Room만 읽으므로 오프라인에서도 동작한다.
 */
class MarketRepository(
    context: Context,
    private val database: PetlingDatabase,
    private val marketDao: MarketDao,
    private val priceDao: PriceDao,
    private val clock: AppClock,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    data class SyncState(
        val lastSyncMillis: Long?,
        val latestDay: String?,
        val productCount: Int,
        val syncing: Boolean = false,
        val lastError: String? = null,
    ) {
        val hasData: Boolean get() = productCount > 0 && latestDay != null
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val syncMutex = Mutex()

    private val _state = MutableStateFlow(
        SyncState(
            lastSyncMillis = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
            latestDay = prefs.getString(KEY_LATEST_DAY, null),
            productCount = prefs.getInt(KEY_PRODUCT_COUNT, 0),
        ),
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /** 앱 시작 시: 데이터가 없거나 [maxAgeMillis]보다 오래됐으면 받는다. 실패는 조용히(다음 기회에). */
    suspend fun syncIfStale(maxAgeMillis: Long = DEFAULT_MAX_AGE_MS) {
        val s = _state.value
        val stale = !s.hasData || s.lastSyncMillis == null || clock.nowMillis() - s.lastSyncMillis > maxAgeMillis
        if (stale) sync()
    }

    /** 게시본을 받아 Room을 통째로 교체한다. 설정 화면 "지금 갱신"도 이걸 부른다. */
    suspend fun sync(): Result<Unit> = syncMutex.withLock {
        _state.value = _state.value.copy(syncing = true, lastError = null)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val index = json.decodeFromString(IndexDto.serializer(), download("index.json"))
                val trend = json.decodeFromString(TrendDto.serializer(), download("trend.json"))
                val stores = index.stores.map {
                    MarketStoreEntity(it.id, it.name, it.type, it.area, it.areaDetail, it.addr)
                }
                val products = index.products.map {
                    MarketProductEntity(
                        id = it.id,
                        name = it.name,
                        normalizedName = MarketMatcher.normalize(MarketMatcher.coreName(it.name)),
                        totalAmount = it.total,
                        totalUnit = it.totalUnit,
                        cls = it.cls,
                    )
                }
                val medians = trend.products.flatMap { (goodId, byDay) ->
                    byDay.flatMap { (day, byType) ->
                        byType.map { (type, price) -> MarketMedianEntity(goodId.toLong(), day, type, price) }
                    }
                }
                database.withTransaction {
                    marketDao.clearStores()
                    marketDao.clearProducts()
                    marketDao.upsertStores(stores)
                    marketDao.upsertProducts(products)
                    marketDao.upsertMedians(medians)
                }
                val latestDay = index.latestDay ?: marketDao.latestDay()
                prefs.edit()
                    .putLong(KEY_LAST_SYNC, clock.nowMillis())
                    .putString(KEY_LATEST_DAY, latestDay)
                    .putInt(KEY_PRODUCT_COUNT, products.size)
                    .apply()
                _state.value = SyncState(clock.nowMillis(), latestDay, products.size)
            }
        }
        result.onFailure { e ->
            _state.value = _state.value.copy(syncing = false, lastError = e.message ?: e.javaClass.simpleName)
        }
        result
    }

    /**
     * 확인 화면용 시장 비교. [marketGoodId]가 있으면(재방문 상품에 저장된 매핑 또는 사용자가 고른 후보) 그걸 쓰고,
     * 없으면 이름·용량으로 매칭한다. 참가격에 없는 상품이면 null.
     */
    suspend fun insightFor(
        tagName: String,
        tagAmount: Double?,
        tagUnit: String?,
        marketGoodId: Long?,
        storeName: String?,
    ): MarketInsight? {
        if (!_state.value.hasData) return null
        if (marketGoodId != null) return insightForGoodId(marketGoodId, storeName, pickedByUser = true)
        val match = MarketMatcher.match(tagName, tagAmount, tagUnit, marketDao.allProducts())
        val best = match.best ?: return null
        return build(best.product, storeName, match, confident = match.confident)
    }

    /** 사용자가 후보를 골랐을 때(또는 저장된 매핑). */
    suspend fun insightForGoodId(goodId: Long, storeName: String?, pickedByUser: Boolean = true): MarketInsight? {
        val product = marketDao.product(goodId) ?: return null
        return build(product, storeName, MarketMatch(listOf(MarketCandidate(product, 1.0)), true), confident = pickedByUser)
    }

    private suspend fun build(
        product: MarketProductEntity,
        storeName: String?,
        match: MarketMatch,
        confident: Boolean,
    ): MarketInsight? {
        val medians = marketDao.medians(product.id)
        val latestDay = medians.maxOfOrNull { it.day } ?: return null
        val latest = medians.filter { it.day == latestDay }.associate { it.type to it.priceWon }
        return MarketInsight(
            product = product,
            latestDay = latestDay,
            medianByType = latest,
            storeType = storeTypeFor(storeName),
            candidates = match.candidates,
            confident = confident,
        )
    }

    /** 사용자가 고른 매장명으로 참가격 업태 추정("이마트 성수점" → LM). 없으면 null. */
    suspend fun storeTypeFor(storeName: String?): String? {
        val wanted = normalizeStoreName(storeName) ?: return null
        val stores = marketDao.allStores()
        // 정확 포함 → 앞 2글자 브랜드 일치 순
        stores.firstOrNull { normalizeStoreName(it.name)?.let { n -> n.contains(wanted) || wanted.contains(n) } == true }
            ?.let { return it.type }
        val brand = wanted.take(3)
        return stores.firstOrNull { normalizeStoreName(it.name)?.startsWith(brand) == true }?.type
    }

    /** 영수증 일괄 저장 시 매칭용 전체 상품(데이터 없으면 빈 목록). */
    suspend fun allProductsForMatching(): List<MarketProductEntity> =
        if (_state.value.hasData) marketDao.allProducts() else emptyList()

    /** 상품 상세용: 저장된 매핑의 중앙값 이력. */
    fun observeMedians(goodId: Long) = marketDao.observeMedians(goodId)

    /** 시세 상세용: 이 참가격 상품에 매핑된 내 기록. */
    fun observeMyEntries(goodId: Long) = priceDao.observeEntriesForMarketGood(goodId)

    /**
     * 물가 개요: 대표 생필품의 최신 전체 중앙값과 직전 조사 대비 변동, 그리고 내린/오른 상품 TOP.
     * 기록이 0건인 사용자에게도 "열면 바로 답"을 주기 위한 화면의 데이터. 조사일이 1개면 변동은 null.
     */
    suspend fun overview(topN: Int = 5): MarketOverview? {
        if (!_state.value.hasData) return null
        val days = marketDao.days()
        val latestDay = days.lastOrNull() ?: return null
        val prevDay = days.getOrNull(days.size - 2)
        val rows = marketDao.mediansOfType("ALL", listOfNotNull(latestDay, prevDay))
        val latest = rows.filter { it.day == latestDay }.associate { it.goodId to it.priceWon }
        val prev = if (prevDay == null) emptyMap() else rows.filter { it.day == prevDay }.associate { it.goodId to it.priceWon }

        fun changeOf(goodId: Long): MarketChange? {
            val now = latest[goodId] ?: return null
            val before = prev[goodId]
            return MarketChange(now, before, before?.let { if (it > 0) (now - it) * 100.0 / it else null })
        }

        val staples = STAPLE_FRAGMENTS.mapNotNull { fragment ->
            val p = marketDao.firstProductLike(fragment) ?: return@mapNotNull null
            changeOf(p.id)?.let { MarketOverviewItem(p, it) }
        }

        val movers = if (prevDay == null) emptyList() else {
            val products = marketDao.allProducts().associateBy { it.id }
            latest.keys.mapNotNull { id ->
                val c = changeOf(id) ?: return@mapNotNull null
                val pct = c.changePct ?: return@mapNotNull null
                val p = products[id] ?: return@mapNotNull null
                if (kotlin.math.abs(pct) < 1.0) null else MarketOverviewItem(p, c)
            }
        }
        return MarketOverview(
            latestDay = latestDay,
            previousDay = prevDay,
            staples = staples,
            down = movers.filter { (it.change.changePct ?: 0.0) < 0 }.sortedBy { it.change.changePct }.take(topN),
            up = movers.filter { (it.change.changePct ?: 0.0) > 0 }.sortedByDescending { it.change.changePct }.take(topN),
        )
    }

    /** 시세 검색: 이름 부분 일치 + 최신 조사일의 전체/대형마트 중앙값. */
    suspend fun search(query: String, limit: Int = 40): List<MarketSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val products = marketDao.searchProducts(q, MarketMatcher.normalize(q), limit)
        if (products.isEmpty()) return emptyList()
        val latest = marketDao.latestMedians().groupBy { it.goodId }
        return products.map { p ->
            val byType = latest[p.id].orEmpty().associate { it.type to it.priceWon }
            MarketSearchItem(p, latestAll = byType["ALL"], latestLM = byType["LM"])
        }
    }

    suspend fun product(goodId: Long): MarketProductEntity? = marketDao.product(goodId)

    /** 사용자가 확인한 매핑을 상품에 기억시켜 다음부턴 자동으로 붙인다. */
    suspend fun rememberMapping(productId: Long, goodId: Long?) {
        priceDao.setMarketGoodId(productId, goodId)
    }

    suspend fun searchStoreNames(query: String, limit: Int = 6): List<String> =
        if (query.isBlank()) emptyList() else marketDao.searchStoreNames(query.trim(), limit)

    private fun download(file: String): String {
        val conn = (URL(baseUrl + file).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("User-Agent", "martmemo-android")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code ($file)")
            val raw = conn.inputStream
            val stream = if (conn.contentEncoding?.contains("gzip") == true) GZIPInputStream(raw) else raw
            return stream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ── 게시본 JSON 형태(martmemo-data/scripts/fetch_prices.py 출력과 일치)
    @Serializable
    private data class IndexDto(
        val latestDay: String? = null,
        val days: List<String> = emptyList(),
        val stores: List<StoreDto> = emptyList(),
        val products: List<ProductDto> = emptyList(),
    )

    @Serializable
    private data class StoreDto(
        val id: Long,
        val name: String,
        val type: String = "?",
        val area: String? = null,
        val areaDetail: String? = null,
        val addr: String? = null,
    )

    @Serializable
    private data class ProductDto(
        val id: Long,
        val name: String,
        val total: Double? = null,
        val totalUnit: String? = null,
        val cls: String? = null,
    )

    @Serializable
    private data class TrendDto(
        val days: List<String> = emptyList(),
        /** goodId → day → type → 중앙값 */
        val products: Map<String, Map<String, Map<String, Int>>> = emptyMap(),
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://novoodi.github.io/martmemo-data/data/"
        const val SOURCE_LABEL = "한국소비자원 참가격 · 격주 조사 · 정가 기준(행사가 미반영)"
        private const val DEFAULT_MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000
        private const val PREFS = "martmemo"
        private const val KEY_LAST_SYNC = "market_last_sync"
        private const val KEY_LATEST_DAY = "market_latest_day"
        private const val KEY_PRODUCT_COUNT = "market_product_count"

        val TYPE_LABELS = mapOf("LM" to "대형마트", "SM" to "SSM", "DP" to "백화점", "CS" to "편의점", "ALL" to "전체")

        /** 홈 "이번 주 시장 물가" 대표 상품 — 게시본 상품명의 부분 문자열로 고른다(id 하드코딩 회피). */
        val STAPLE_FRAGMENTS = listOf(
            "깨끗한 계란(15개)", "서울우유 흰우유(1L)", "신라면(5개입)", "이천쌀", "국산콩두부 찌개용",
            "전주 콩나물", "해표 맑고 신선한 식용유", "백설 하얀설탕", "비비고 포기배추김치", "순수소프트 3겹 화장지",
        )
    }
}

/** 최신 전체 중앙값과 직전 조사 대비 변동. [changePct]는 직전 조사가 없으면 null. */
data class MarketChange(val nowWon: Int, val beforeWon: Int?, val changePct: Double?)

data class MarketOverviewItem(val product: MarketProductEntity, val change: MarketChange)

data class MarketOverview(
    val latestDay: String,
    val previousDay: String?,
    val staples: List<MarketOverviewItem>,
    val down: List<MarketOverviewItem>,
    val up: List<MarketOverviewItem>,
)

/** 시세 검색 결과 한 줄. */
data class MarketSearchItem(
    val product: MarketProductEntity,
    val latestAll: Int?,
    val latestLM: Int?,
)

/** 확인 화면 "시장 가격" 카드 내용. */
data class MarketInsight(
    val product: MarketProductEntity,
    /** 조사일 YYYYMMDD. */
    val latestDay: String,
    /** 업태 코드(LM/SM/DP/CS/ALL) → 중앙값. */
    val medianByType: Map<String, Int>,
    /** 사용자가 고른 매장의 업태 추정. 없으면 ALL 기준으로 비교. */
    val storeType: String?,
    /** 사용자에게 보여줄 후보(확정이 아닐 때). */
    val candidates: List<MarketCandidate>,
    /** true면 자동 확정 또는 사용자 선택. false면 후보 칩을 보여준다. */
    val confident: Boolean,
) {
    /** 비교 기준 업태와 그 중앙값. 매장 업태를 모르거나 그 업태 값이 없으면 전체 중앙값. */
    val reference: Pair<String, Int>?
        get() {
            val t = storeType?.takeIf { medianByType.containsKey(it) } ?: "ALL"
            return medianByType[t]?.let { t to it }
        }
}
