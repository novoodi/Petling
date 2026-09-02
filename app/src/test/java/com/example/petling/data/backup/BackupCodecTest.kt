package com.example.petling.data.backup

import com.example.petling.data.local.entity.PriceEntryEntity
import com.example.petling.data.local.entity.PriceProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {

    private val eggs = PriceProductEntity(
        id = 1, name = "특란 30구", normalizedName = "특란30구",
        volumeAmount = 30.0, volumeUnit = "구", barcode = "8801234567890", createdAt = 100,
    )
    private val milk = PriceProductEntity(
        id = 2, name = "서울우유 1L", normalizedName = "서울우유1l", createdAt = 200,
    )
    private val entries = listOf(
        PriceEntryEntity(id = 10, productId = 1, priceWon = 6_980, storeName = "이마트", dateEpochDay = 20_000, createdAt = 110),
        PriceEntryEntity(id = 11, productId = 1, priceWon = 7_480, originalPriceWon = 7_980, dateEpochDay = 20_014, createdAt = 120),
        PriceEntryEntity(id = 12, productId = 2, priceWon = 2_980, unitPriceWon = 298, unitBaseAmount = 100.0, unitBaseUnit = "ml", dateEpochDay = 20_014, createdAt = 210),
        // 상품이 없는 고아 기록은 내보내기에서 빠진다
        PriceEntryEntity(id = 13, productId = 99, priceWon = 1_000, dateEpochDay = 20_014, createdAt = 300),
    )

    @Test
    fun `내보내기 후 빈 DB에 가져오면 상품·기록이 동일하게 복원된다`() {
        val text = BackupCodec.encode(listOf(eggs, milk), entries, exportedAt = 1_000, appVersion = "2.0")
        val file = BackupCodec.decode(text)
        assertEquals("2.0", file.appVersion)
        assertEquals(2, file.products.size)
        assertEquals(3, file.entries.size)

        val plan = BackupCodec.plan(file, existingProducts = emptyList(), existingEntries = emptyMap())
        assertEquals(2, plan.size)
        plan.forEach { assertNull(it.existing); assertNotNull(it.toCreate) }

        val eggPlan = plan.first { it.toCreate!!.name == "특란 30구" }
        assertEquals("8801234567890", eggPlan.toCreate!!.barcode)
        assertEquals(30.0, eggPlan.toCreate!!.volumeAmount!!, 0.0)
        assertEquals(listOf(6_980, 7_480), eggPlan.entries.map { it.priceWon })
        assertEquals("이마트", eggPlan.entries[0].storeName)
        assertEquals(7_980, eggPlan.entries[1].originalPriceWon)
        assertEquals(0, eggPlan.skipped)

        val milkPlan = plan.first { it.toCreate!!.name == "서울우유 1L" }
        assertEquals(298, milkPlan.entries.single().unitPriceWon)
        assertEquals("ml", milkPlan.entries.single().unitBaseUnit)
    }

    @Test
    fun `같은 파일을 두 번 가져오면 전부 중복으로 건너뛴다`() {
        val text = BackupCodec.encode(listOf(eggs, milk), entries, 1_000, "2.0")
        val file = BackupCodec.decode(text)
        // 기존 DB에는 id가 달라도 바코드/이름이 같은 상품과 같은 기록이 있다
        val existingEggs = eggs.copy(id = 50)
        val existingMilk = milk.copy(id = 51)
        val existingEntries = mapOf(
            50L to entries.filter { it.productId == 1L }.map { it.copy(productId = 50) },
            51L to entries.filter { it.productId == 2L }.map { it.copy(productId = 51) },
        )
        val plan = BackupCodec.plan(file, listOf(existingEggs, existingMilk), existingEntries)
        assertEquals(2, plan.size)
        plan.forEach {
            assertNotNull(it.existing)
            assertNull(it.toCreate)
            assertEquals(0, it.entries.size)
        }
        assertEquals(3, plan.sumOf { it.skipped })
    }

    @Test
    fun `바코드가 없으면 정규화 이름으로 기존 상품에 붙이고 새 기록만 추가한다`() {
        val text = BackupCodec.encode(listOf(milk), entries.filter { it.productId == 2L }, 1_000, "2.0")
        val file = BackupCodec.decode(text)
        val existing = PriceProductEntity(id = 7, name = "서울 우유 1L", normalizedName = "서울우유1l", createdAt = 1)
        val plan = BackupCodec.plan(
            file,
            listOf(existing),
            mapOf(7L to listOf(PriceEntryEntity(productId = 7, priceWon = 2_780, dateEpochDay = 19_990, createdAt = 5))),
        )
        val item = plan.single()
        assertEquals(7L, item.existing!!.id)
        assertEquals(listOf(2_980), item.entries.map { it.priceWon })
        assertEquals(0, item.skipped)
    }

    @Test
    fun `다른 형식의 JSON은 거부한다`() {
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("""{"foo": 1}""") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("not json") }
        val newer = """{"format":"martmemo-backup","version":99,"exportedAt":0,"appVersion":"9","products":[],"entries":[]}"""
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(newer) }
    }

    @Test
    fun `모르는 키는 무시한다(상위 버전 호환)`() {
        val text = """{"format":"martmemo-backup","version":1,"exportedAt":0,"appVersion":"2.1","futureField":true,
            "products":[{"id":1,"name":"신라면 5입","createdAt":1,"isStarred":true}],
            "entries":[{"productId":1,"priceWon":3980,"dateEpochDay":20000,"createdAt":2,"marketGoodId":42}]}"""
        val file = BackupCodec.decode(text)
        assertEquals("신라면 5입", file.products.single().name)
        assertEquals(3_980, file.entries.single().priceWon)
    }
}
