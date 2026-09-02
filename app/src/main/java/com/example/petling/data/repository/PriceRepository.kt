package com.example.petling.data.repository

import android.net.Uri
import com.example.petling.data.capture.ImageStore
import com.example.petling.data.capture.OcrTextExtractor
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import com.example.petling.data.market.MarketInsight
import com.example.petling.data.market.MarketRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.price.PriceTagExtractor
import com.example.petling.domain.price.PreviousRecord
import com.example.petling.domain.price.PriceTagInfo
import com.example.petling.domain.price.normalizeProductName
import com.example.petling.domain.price.normalizeStoreName
import com.example.petling.domain.price.previousFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 상품 + 최신 가격 요약(목록 화면용). */
data class TrackedProduct(
    val product: PriceProductEntity,
    val latest: PriceEntryEntity?,
    /** 직전 기록 대비 차액(오늘-지난번). 같은 매장 직전 기록 우선, 없으면 전체 직전 기록. */
    val deltaWon: Int?,
    /** deltaWon이 다른 매장 기록과의 비교인지(같은 매장 기록이 없어 폴백). */
    val deltaOtherStore: Boolean,
    val entryCount: Int,
)

/** 가격표 사진 분석 결과 — 확인 화면 초안. 저장 전까지 DB에 쓰지 않는다. */
data class PriceAnalysis(
    val imagePath: String?,
    val ocrText: String,
    val tag: PriceTagInfo,
    /** 같은 상품으로 매칭된 기존 상품(재방문). */
    val matchedProduct: PriceProductEntity?,
    /** 매칭된 상품의 비교 대상 기록(같은 매장 우선). 매장 선택이 바뀌면 [PriceRepository.withStore]로 갱신. */
    val previous: PreviousRecord?,
    /** 시장 비교(참가격). 참가격에 없는 상품이거나 데이터 미수신이면 null. */
    val market: MarketInsight? = null,
    /** 확정된 참가격 상품 id(저장된 매핑 / 자동 확정 / 사용자 선택). 저장 시 상품에 기억된다. */
    val marketGoodId: Long? = null,
    /** 사용자가 "해당 없음"을 눌러 시장 카드를 닫은 경우. */
    val marketDismissed: Boolean = false,
)

/**
 * 가격 추적 핵심: 가격표 사진 → 추출(온디바이스) → 재방문 매칭 → 기록. 전부 온디바이스.
 * 온라인 시세는 상품명이 채워진 검색 딥링크(네이버쇼핑/쿠팡)로 확인한다 —
 * 네이버 쇼핑 검색 API가 2026-07-31 종료(API HUB 미포함)되어 공식 가격 조회 경로가 없다.
 */
class PriceRepository(
    private val priceDao: PriceDao,
    private val imageStore: ImageStore,
    private val ocr: OcrTextExtractor,
    private val extractor: PriceTagExtractor,
    private val clock: AppClock,
    private val market: MarketRepository,
) {

    fun observeTracked(): Flow<List<TrackedProduct>> =
        combine(priceDao.observeProducts(), priceDao.observeAllEntries()) { products, entries ->
            val byProduct = entries.groupBy { it.productId }
            products.map { product ->
                val list = byProduct[product.id].orEmpty().sortedByDescending { it.createdAt }
                val latest = list.firstOrNull()
                val previous = latest?.let { previousFor(list.drop(1), it.storeName) }
                TrackedProduct(
                    product = product,
                    latest = latest,
                    deltaWon = if (latest != null && previous != null) latest.priceWon - previous.entry.priceWon else null,
                    deltaOtherStore = previous?.sameStore == false,
                    entryCount = list.size,
                )
            }
        }

    fun observeProduct(productId: Long): Flow<PriceProductEntity?> = priceDao.observeProduct(productId)

    fun observeEntries(productId: Long): Flow<List<PriceEntryEntity>> = priceDao.observeEntries(productId)

    /** 확인 화면 매장 칩용 최근 매장(최신순 3개). */
    suspend fun recentStores(): List<String> = priceDao.recentStoreNames(RECENT_STORE_LIMIT)

    /** 오늘 이미 기록한 매장이 있으면 그 매장을 기본 선택한다(같은 장보기 중일 가능성이 높다). */
    suspend fun defaultStoreForToday(): String? =
        priceDao.latestEntryOverall()
            ?.takeIf { it.dateEpochDay == clock.today().toEpochDay() }
            ?.storeName

    /** 사진 한 장을 분석한다(저장 아님). 이미지 저장 실패 시에도 OCR은 시도한다. */
    suspend fun analyze(uri: Uri, storeName: String?): PriceAnalysis {
        val imagePath = imageStore.save(uri)
        val ocrText = ocr.extract(uri)
        val tag = extractor.extract(ocrText, imagePath)

        val matched = findExisting(tag)
        val insight = marketFor(tag, matched?.marketGoodId, storeName)

        return PriceAnalysis(
            imagePath = imagePath,
            ocrText = ocrText,
            tag = tag,
            matchedProduct = matched,
            previous = matched?.let { previousFor(it.id, storeName) },
            market = insight,
            marketGoodId = insight?.takeIf { it.confident }?.product?.id,
        )
    }

    /** 참가격 매칭: 저장된 매핑 우선, 없으면 이름·용량. 데이터가 없거나 실패하면 null(카드 숨김). */
    private suspend fun marketFor(tag: PriceTagInfo, savedGoodId: Long?, storeName: String?): MarketInsight? =
        runCatching {
            market.insightFor(tag.name, tag.volumeAmount, tag.volumeUnit, savedGoodId, storeName)
        }.getOrNull()

    /** 사용자가 시장 후보를 골랐거나(goodId) "해당 없음"(null)을 눌렀을 때. */
    suspend fun withMarketPick(analysis: PriceAnalysis, goodId: Long?, storeName: String?): PriceAnalysis {
        if (goodId == null) return analysis.copy(market = null, marketGoodId = null, marketDismissed = true)
        val insight = runCatching { market.insightForGoodId(goodId, storeName) }.getOrNull()
        return analysis.copy(market = insight, marketGoodId = insight?.product?.id, marketDismissed = false)
    }

    /** 사용자가 이름을 수정했을 때 재방문 매칭을 다시 수행한다. */
    suspend fun requery(analysis: PriceAnalysis, name: String, storeName: String?): PriceAnalysis {
        val tag = analysis.tag.copy(name = name)
        val matched = findExisting(tag)
        val insight = marketFor(tag, matched?.marketGoodId, storeName)
        return analysis.copy(
            tag = tag,
            matchedProduct = matched,
            previous = matched?.let { previousFor(it.id, storeName) },
            market = insight,
            marketGoodId = insight?.takeIf { it.confident }?.product?.id,
            marketDismissed = false,
        )
    }

    /** 매장 선택이 바뀌면 비교 대상 기록과 시장 카드의 업태 기준만 다시 고른다(OCR·매칭은 그대로). */
    suspend fun withStore(analysis: PriceAnalysis, storeName: String?): PriceAnalysis {
        val insight = analysis.market?.let { m ->
            m.copy(storeType = runCatching { market.storeTypeFor(storeName) }.getOrNull())
        }
        return analysis.copy(
            previous = analysis.matchedProduct?.let { previousFor(it.id, storeName) },
            market = insight,
        )
    }

    /** 매장 직접 입력 자동완성(참가격 판매점 이름). */
    suspend fun storeSuggestions(query: String): List<String> =
        runCatching { market.searchStoreNames(query) }.getOrDefault(emptyList())

    private suspend fun previousFor(productId: Long, storeName: String?): PreviousRecord? =
        previousFor(priceDao.entriesFor(productId), storeName)

    /** 바코드 → 정규화 이름 순으로 기존 상품을 찾는다. */
    private suspend fun findExisting(tag: PriceTagInfo): PriceProductEntity? {
        tag.barcode?.let { priceDao.findByBarcode(it)?.let { p -> return p } }
        val normalized = normalizeProductName(tag.name)
        if (normalized.isBlank()) return null
        return priceDao.findByNormalizedName(normalized)
    }

    /**
     * 확인된 분석 결과를 저장한다(상품 upsert + 기록 insert + 캐릭터 성장).
     * @return 저장된 상품 id. 가격이 없으면 null(저장 불가).
     */
    suspend fun save(analysis: PriceAnalysis, name: String, priceWon: Int, storeName: String?): Long? {
        if (name.isBlank() || priceWon <= 0) return null
        // 매장은 표기 그대로 저장하되 빈 값은 null. 비교는 normalizeStoreName 기준.
        val store = storeName?.trim()?.takeIf { normalizeStoreName(it) != null }
        val now = clock.nowMillis()
        val tag = analysis.tag

        val productId = analysis.matchedProduct?.let { existing ->
            // 재방문: 바코드가 새로 읽혔으면 채워준다(다음 매칭 정확도 향상).
            if (existing.barcode == null && tag.barcode != null) {
                priceDao.updateProduct(existing.copy(barcode = tag.barcode))
            }
            existing.id
        } ?: priceDao.insertProduct(
            PriceProductEntity(
                name = name,
                normalizedName = normalizeProductName(name),
                volumeAmount = tag.volumeAmount,
                volumeUnit = tag.volumeUnit,
                barcode = tag.barcode,
                createdAt = now,
            )
        )

        // 시장 매핑 기억: 다음 촬영부터 이름 매칭 없이 바로 붙는다
        analysis.marketGoodId?.let { goodId ->
            if (analysis.matchedProduct?.marketGoodId != goodId) market.rememberMapping(productId, goodId)
        }

        priceDao.insertEntry(
            PriceEntryEntity(
                productId = productId,
                priceWon = priceWon,
                originalPriceWon = tag.originalPriceWon,
                unitPriceWon = tag.unitPriceWon,
                unitBaseAmount = tag.unitBaseAmount,
                unitBaseUnit = tag.unitBaseUnit,
                saleEndEpochDay = tag.saleEndEpochDay,
                storeName = store,
                imagePath = analysis.imagePath,
                dateEpochDay = clock.today().toEpochDay(),
                createdAt = now,
            )
        )

        return productId
    }

    /** 분석을 저장하지 않고 버릴 때 저장해둔 이미지를 정리한다. */
    fun discard(analysis: PriceAnalysis) {
        analysis.imagePath?.let { imageStore.delete(it) }
    }

    suspend fun deleteProduct(productId: Long) {
        priceDao.entriesFor(productId).forEach { it.imagePath?.let(imageStore::delete) }
        priceDao.deleteEntriesFor(productId)
        priceDao.deleteProduct(productId)
    }

    private companion object {
        const val RECENT_STORE_LIMIT = 3
    }
}
