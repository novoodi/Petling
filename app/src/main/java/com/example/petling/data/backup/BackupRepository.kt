package com.example.petling.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.local.dao.PriceDao
import com.example.petling.domain.AppClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 백업 파일 내보내기/가져오기. 저장 위치는 시스템 파일 선택기가 준 Uri라 권한이 필요 없다.
 * allowBackup=false(자동 백업은 기기 간 예측 불가)라 이 수동 파일이 진실원이다.
 */
class BackupRepository(
    private val context: Context,
    private val database: PetlingDatabase,
    private val priceDao: PriceDao,
    private val clock: AppClock,
    private val appVersion: String,
) {

    /** 파일 선택기에 제안할 기본 파일명. */
    fun suggestedFileName(): String = "마트메모_백업_${LocalDate.ofEpochDay(clock.today().toEpochDay())}.json"

    /** @return 내보낸 (상품 수, 기록 수). 실패 시 예외. */
    suspend fun exportTo(uri: Uri): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val products = priceDao.allProducts()
        val entries = priceDao.allEntries()
        val text = BackupCodec.encode(products, entries, clock.nowMillis(), appVersion)
        // "wt": 기존 파일을 고를 수 있으므로 잘라 쓰기(append·덮어쓰기 잔여 방지)
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("파일을 열 수 없어요")
        out.bufferedWriter().use { it.write(text) }
        products.size to entries.size
    }

    /**
     * 병합 가져오기: 바코드·정규화 이름으로 기존 상품에 붙이고 중복 기록은 건너뛴다.
     * 전체를 한 트랜잭션으로 처리해 중간 실패 시 아무것도 남지 않게 한다.
     * @throws BackupFormatException 형식 오류
     */
    suspend fun importFrom(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("파일을 읽을 수 없어요")
        val file = BackupCodec.decode(text)

        database.withTransaction {
            val existingProducts = priceDao.allProducts()
            val existingEntries = priceDao.allEntries().groupBy { it.productId }
            val plan = BackupCodec.plan(file, existingProducts, existingEntries)

            var productsAdded = 0
            var entriesAdded = 0
            var skipped = 0
            for (item in plan) {
                val productId = item.existing?.id
                    ?: priceDao.insertProduct(requireNotNull(item.toCreate)).also { productsAdded++ }
                item.entries.forEach { priceDao.insertEntry(it.copy(productId = productId)) }
                entriesAdded += item.entries.size
                skipped += item.skipped
            }
            ImportSummary(productsAdded, entriesAdded, skipped)
        }
    }
}
