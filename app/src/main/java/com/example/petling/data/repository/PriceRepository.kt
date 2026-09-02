package com.example.petling.data.repository

import android.net.Uri
import com.example.petling.data.capture.ImageStore
import com.example.petling.data.capture.OcrTextExtractor
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import com.example.petling.domain.AppClock
import com.example.petling.domain.price.PriceTagExtractor
import com.example.petling.domain.price.PriceTagInfo
import com.example.petling.domain.price.normalizeProductName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 상품 + 최신 가격 요약(목록 화면용). */
data class TrackedProduct(
    val product: PriceProductEntity,
    val latest: PriceEntryEntity?,
    /** 직전 기록 대비 차액(오늘-지난번). 기록이 2개 이상일 때만. */
    val deltaWon: Int?,
    val entryCount: Int,
)

/** 가격표 사진 분석 결과 — 확인 화면 초안. 저장 전까지 DB에 쓰지 않는다. */
data class PriceAnalysis(
    val imagePath: String?,
    val ocrText: String,
    val tag: PriceTagInfo,
    /** 같은 상품으로 매칭된 기존 상품(재방문). */
    val matchedProduct: PriceProductEntity?,
    /** 매칭된 상품의 직전 기록(재방문 비교용). */
    val previousEntry: PriceEntryEntity?,
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
) {

    fun observeTracked(): Flow<List<TrackedProduct>> =
        combine(priceDao.observeProducts(), priceDao.observeAllEntries()) { products, entries ->
            val byProduct = entries.groupBy { it.productId }
            products.map { product ->
                val list = byProduct[product.id].orEmpty().sortedByDescending { it.createdAt }
                TrackedProduct(
                    product = product,
                    latest = list.firstOrNull(),
                    deltaWon = if (list.size >= 2) list[0].priceWon - list[1].priceWon else null,
                    entryCount = list.size,
                )
            }
        }

    fun observeProduct(productId: Long): Flow<PriceProductEntity?> = priceDao.observeProduct(productId)

    fun observeEntries(productId: Long): Flow<List<PriceEntryEntity>> = priceDao.observeEntries(productId)

    /** 사진 한 장을 분석한다(저장 아님). 이미지 저장 실패 시에도 OCR은 시도한다. */
    suspend fun analyze(uri: Uri): PriceAnalysis {
        val imagePath = imageStore.save(uri)
        val ocrText = ocr.extract(uri)
        val tag = extractor.extract(ocrText, imagePath)

        val matched = findExisting(tag)
        val previous = matched?.let { priceDao.latestEntry(it.id) }

        return PriceAnalysis(
            imagePath = imagePath,
            ocrText = ocrText,
            tag = tag,
            matchedProduct = matched,
            previousEntry = previous,
        )
    }

    /** 사용자가 이름을 수정했을 때 재방문 매칭을 다시 수행한다. */
    suspend fun requery(analysis: PriceAnalysis, name: String): PriceAnalysis {
        val tag = analysis.tag.copy(name = name)
        val matched = findExisting(tag)
        val previous = matched?.let { priceDao.latestEntry(it.id) }
        return analysis.copy(
            tag = tag,
            matchedProduct = matched,
            previousEntry = previous,
        )
    }

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
    suspend fun save(analysis: PriceAnalysis, name: String, priceWon: Int): Long? {
        if (name.isBlank() || priceWon <= 0) return null
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

        priceDao.insertEntry(
            PriceEntryEntity(
                productId = productId,
                priceWon = priceWon,
                originalPriceWon = tag.originalPriceWon,
                unitPriceWon = tag.unitPriceWon,
                unitBaseAmount = tag.unitBaseAmount,
                unitBaseUnit = tag.unitBaseUnit,
                saleEndEpochDay = tag.saleEndEpochDay,
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
}
