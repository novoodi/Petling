package com.example.petling.data.backup

import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import com.example.petling.domain.price.normalizeProductName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 백업 파일 형식(JSON 한 파일). 사진은 제외한다(용량·복잡도).
 * 온디바이스 전용 앱이라 기기 교체 시 이 파일이 유일한 복구 수단이다 — 필드 삭제·의미 변경 금지,
 * 추가만 하고 [FORMAT_VERSION]을 올린다. 읽을 때는 모르는 키를 무시한다.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT_NAME,
    val version: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val products: List<BackupProduct>,
    val entries: List<BackupEntry>,
) {
    companion object {
        const val FORMAT_NAME = "martmemo-backup"
        const val FORMAT_VERSION = 1
    }
}

@Serializable
data class BackupProduct(
    /** 파일 안에서만 유효한 id — entries.productId가 참조. 가져올 때 새 id로 매핑된다. */
    val id: Long,
    val name: String,
    val volumeAmount: Double? = null,
    val volumeUnit: String? = null,
    val barcode: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupEntry(
    val productId: Long,
    val priceWon: Int,
    val originalPriceWon: Int? = null,
    val unitPriceWon: Int? = null,
    val unitBaseAmount: Double? = null,
    val unitBaseUnit: String? = null,
    val saleEndEpochDay: Long? = null,
    val storeName: String? = null,
    val dateEpochDay: Long,
    val createdAt: Long,
)

/** 가져오기 결과 요약(설정 화면 안내용). */
data class ImportSummary(
    val productsAdded: Int,
    val entriesAdded: Int,
    val entriesSkipped: Int,
)

/**
 * 가져오기 계획 한 상품분. [existing]이 있으면 그 상품에 기록을 붙이고, 없으면 [toCreate]를 새로 만든다.
 * [entries]는 productId가 비어 있으며(0) 실행 시 실제 id로 채운다.
 */
data class ImportItem(
    val existing: PriceProductEntity?,
    val toCreate: PriceProductEntity?,
    val entries: List<PriceEntryEntity>,
    val skipped: Int,
)

class BackupFormatException(message: String) : IllegalArgumentException(message)

/** 순수 Kotlin: 엔티티 ↔ JSON 변환과 병합 계획. DB 접근은 [BackupRepository]가 한다. */
object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(
        products: List<PriceProductEntity>,
        entries: List<PriceEntryEntity>,
        exportedAt: Long,
        appVersion: String,
    ): String {
        val ids = products.map { it.id }.toSet()
        val file = BackupFile(
            exportedAt = exportedAt,
            appVersion = appVersion,
            products = products.map {
                BackupProduct(
                    id = it.id,
                    name = it.name,
                    volumeAmount = it.volumeAmount,
                    volumeUnit = it.volumeUnit,
                    barcode = it.barcode,
                    createdAt = it.createdAt,
                )
            },
            // 상품이 없는 고아 기록은 복구해도 보일 곳이 없으므로 제외
            entries = entries.filter { it.productId in ids }.map {
                BackupEntry(
                    productId = it.productId,
                    priceWon = it.priceWon,
                    originalPriceWon = it.originalPriceWon,
                    unitPriceWon = it.unitPriceWon,
                    unitBaseAmount = it.unitBaseAmount,
                    unitBaseUnit = it.unitBaseUnit,
                    saleEndEpochDay = it.saleEndEpochDay,
                    storeName = it.storeName,
                    dateEpochDay = it.dateEpochDay,
                    createdAt = it.createdAt,
                )
            },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** @throws BackupFormatException 마트메모 백업 파일이 아니거나 읽을 수 없는 버전일 때. */
    fun decode(text: String): BackupFile {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .getOrElse { throw BackupFormatException("JSON 해석 실패: ${it.message}") }
        if (file.format != BackupFile.FORMAT_NAME) {
            throw BackupFormatException("마트메모 백업 파일이 아니에요")
        }
        if (file.version > BackupFile.FORMAT_VERSION) {
            throw BackupFormatException("더 새로운 버전의 앱에서 만든 파일이에요 — 앱을 업데이트해주세요")
        }
        return file
    }

    /**
     * 병합 계획: 바코드 → 정규화 이름 순으로 기존 상품에 붙이고,
     * 같은 상품에 같은 (dateEpochDay, priceWon) 기록이 이미 있으면 건너뛴다(재가져오기 안전).
     * @param existingEntries 기존 상품 id → 기록 목록
     */
    fun plan(
        file: BackupFile,
        existingProducts: List<PriceProductEntity>,
        existingEntries: Map<Long, List<PriceEntryEntity>>,
    ): List<ImportItem> {
        val byBarcode = existingProducts.filter { it.barcode != null }.associateBy { it.barcode!! }
        val byName = existingProducts.associateBy { it.normalizedName }
        val entriesByProduct = file.entries.groupBy { it.productId }

        return file.products.map { bp ->
            val normalized = normalizeProductName(bp.name)
            val existing = bp.barcode?.let { byBarcode[it] }
                ?: normalized.takeIf { it.isNotBlank() }?.let { byName[it] }
            val seen = existing?.let { existingEntries[it.id].orEmpty() }
                .orEmpty()
                .map { it.dateEpochDay to it.priceWon }
                .toMutableSet()

            val incoming = entriesByProduct[bp.id].orEmpty()
            val toInsert = incoming.filter { seen.add(it.dateEpochDay to it.priceWon) }
            ImportItem(
                existing = existing,
                toCreate = if (existing == null) PriceProductEntity(
                    name = bp.name,
                    normalizedName = normalized,
                    volumeAmount = bp.volumeAmount,
                    volumeUnit = bp.volumeUnit,
                    barcode = bp.barcode,
                    createdAt = bp.createdAt,
                ) else null,
                entries = toInsert.map {
                    PriceEntryEntity(
                        productId = 0L,
                        priceWon = it.priceWon,
                        originalPriceWon = it.originalPriceWon,
                        unitPriceWon = it.unitPriceWon,
                        unitBaseAmount = it.unitBaseAmount,
                        unitBaseUnit = it.unitBaseUnit,
                        saleEndEpochDay = it.saleEndEpochDay,
                        storeName = it.storeName,
                        dateEpochDay = it.dateEpochDay,
                        createdAt = it.createdAt,
                    )
                },
                skipped = incoming.size - toInsert.size,
            )
        }
    }
}
