package com.example.petling.data.repository

import android.net.Uri
import android.util.Log
import com.example.petling.data.capture.ImageStore
import com.example.petling.data.capture.OcrTextExtractor
import com.example.petling.data.local.dao.CaptureDao
import com.example.petling.data.local.entity.CaptureEntity
import com.example.petling.data.local.entity.toDomain
import com.example.petling.data.local.entity.toEntity
import com.example.petling.domain.AppClock
import com.example.petling.domain.capture.CaptureClassifier
import com.example.petling.domain.capture.CaptureSummarizer
import com.example.petling.domain.capture.ScheduleReclassifier
import com.example.petling.domain.capture.UrlExtractor
import com.example.petling.domain.model.CaptureItem
import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.parsing.IntentParsingStrategy
import com.example.petling.domain.parsing.ScheduleParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 캡처 인제스트 결과 — 인제스트 화면이 정리 결과·캐릭터 리액션·일정 제안에 사용. */
data class CaptureResult(
    val item: CaptureItem,
    val growth: CaptureGrowth?,
    /** 날짜만 있어 수동 등록을 제안할 seed(자동 등록되지 않은 경우). */
    val scheduleSeed: ParsedDraftSeed?,
    /** 비서: 날짜+시간이 뚜렷해 자동으로 캘린더에 등록한 일정 id. */
    val registeredScheduleId: Long? = null,
)

/**
 * 캡처 정리함 핵심. 이미지 저장 → OCR → 일정판정 → 분류 → 보관 → 캐릭터 성장.
 */
class CaptureRepository(
    private val captureDao: CaptureDao,
    private val imageStore: ImageStore,
    private val ocr: OcrTextExtractor,
    private val parser: ScheduleParser,
    private val classifier: CaptureClassifier,
    private val summarizer: CaptureSummarizer,
    private val categoryRepository: CategoryRepository,
    private val characterRepository: CharacterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val clock: AppClock,
) {

    private companion object {
        const val TAG = "CapturePipeline"

        /** 이 미만 confidence seed는 일정 제안·자동 등록 후보에서 제외(weak 오탐 흡수). */
        const val MIN_SEED_CONFIDENCE = 0.35f
    }

    fun observeAll(): Flow<List<CaptureItem>> =
        captureDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByKey(key: String): Flow<List<CaptureItem>> =
        captureDao.observeByKey(key).map { list -> list.map { it.toDomain() } }

    fun search(query: String): Flow<List<CaptureItem>> =
        captureDao.search(query).map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<CaptureItem?> =
        captureDao.observeById(id).map { it?.toDomain() }

    suspend fun getById(id: Long): CaptureItem? = captureDao.getById(id)?.toDomain()

    /** 이미지 한 장을 정리함에 보관하고 캐릭터를 성장시킨다. 이미지 저장 실패 시 null. */
    suspend fun ingest(uri: Uri): CaptureResult? {
        val imagePath = imageStore.save(uri) ?: return null
        val ocrText = ocr.extract(uri)

        // 1) 의도 분류 먼저 — 의도가 파싱 정책을 결정한다
        val categories = categoryRepository.enabledForClassify()
        val firstPass = classifier.classify(ocrText, categories)
        val baseType = categories.firstOrNull { it.key == firstPass.categoryKey }?.baseType
            ?: CaptureType.MEMORY

        // 2) 의도별 정책을 적용해 파싱 (로그에 사용자 콘텐츠 금지 — 정책 필드만)
        val policy = IntentParsingStrategy.policyFor(baseType, firstPass.confidence)
        Log.d(
            TAG,
            "policy base=$baseType conf=${firstPass.confidence} strip=${policy.stripPatterns.size} " +
                "ruleOnly=${policy.ruleOnly} suppress=${policy.suppressSeedsBelow} " +
                "mul=${policy.confidenceMul} carry=${policy.dateCarryAcrossLines}",
        )
        val parseText = IntentParsingStrategy.applyStrip(policy, ocrText)
        val seeds = IntentParsingStrategy.postProcess(policy, parser.parse(parseText, policy.toHint()))

        // "일정으로 등록"용 seed는 날짜만 있어도 됨(종일 일정 가능) — weak 오탐(conf<=0.3)은 제외
        val scheduleSeed = seeds.firstOrNull { it.date != null && it.confidence >= MIN_SEED_CONFIDENCE }
        // 날짜 AND 시간이 뚜렷한 seed → 비서가 자동 등록
        val timedSeed = seeds.firstOrNull { it.date != null && it.startMinuteOfDay != null }

        // 3) 2-pass: 강한 파싱 결과가 나오면 catch-all/저신뢰 분류를 일정으로 재조정
        val lineCount = parseText.lineSequence().count { it.isNotBlank() }
        val classification = ScheduleReclassifier.maybePromote(firstPass, timedSeed, lineCount, categories)
        if (classification.categoryKey != firstPass.categoryKey) {
            Log.d(TAG, "reclassified ${firstPass.categoryKey} -> ${classification.categoryKey}")
        }

        val linkUrl = UrlExtractor.firstUrl(ocrText)

        // 캡처 insert 전에 이 카테고리가 처음인지 판정(다양성 보너스)
        val isNewType = captureDao.countByKey().none { it.categoryKey == classification.categoryKey }

        // 비서: 날짜+시간이 뚜렷하면 종류와 무관하게 캘린더 자동 등록(하루 전·당일 알림 자동)
        val registeredScheduleId: Long? = timedSeed?.date?.let {
            scheduleRepository.create(
                Schedule(
                    title = timedSeed.title?.ifBlank { classification.title } ?: classification.title,
                    date = it,
                    startMinuteOfDay = timedSeed.startMinuteOfDay,
                    location = timedSeed.location,
                    category = timedSeed.category,
                    isImportant = timedSeed.isImportant,
                    source = timedSeed.source,
                    reminderOffsetMin = 0,
                ),
            )
        }

        val id = captureDao.insert(
            CaptureEntity(
                imagePath = imagePath,
                ocrText = ocrText,
                categoryKey = classification.categoryKey,
                title = classification.title,
                note = null,
                linkUrl = linkUrl,
                sourceScheduleId = registeredScheduleId,
                pinned = false,
                createdAt = clock.nowMillis(),
            ),
        )
        val growth = characterRepository.recordCapture(classification.categoryKey, isNewType)
        val item = captureDao.getById(id)?.toDomain() ?: return null
        // 자동 등록된 경우엔 수동 등록 제안(seed)을 숨긴다
        return CaptureResult(
            item = item,
            growth = growth,
            scheduleSeed = if (registeredScheduleId == null) scheduleSeed else null,
            registeredScheduleId = registeredScheduleId,
        )
    }

    suspend fun updateCategory(item: CaptureItem, categoryKey: String) {
        captureDao.update(item.toEntity().copy(categoryKey = categoryKey))
    }

    /**
     * 한줄 요약이 없으면 온디바이스 AI로 생성해 저장하고 갱신된 항목을 반환한다.
     * 이미 있거나 요약 불가(비지원)면 그대로 반환.
     */
    suspend fun ensureSummary(item: CaptureItem): CaptureItem {
        if (!item.summary.isNullOrBlank()) return item
        val summary = runCatching { summarizer.summarize(item.ocrText) }.getOrNull() ?: return item
        captureDao.update(item.toEntity().copy(summary = summary))
        return item.copy(summary = summary)
    }

    /** 자동 등록된 일정을 되돌린다(캘린더에서 삭제 + 캡처 연결 해제). */
    suspend fun undoAutoSchedule(item: CaptureItem) {
        item.sourceScheduleId?.let { sid ->
            scheduleRepository.getById(sid)?.let { scheduleRepository.delete(it) }
            captureDao.update(item.toEntity().copy(sourceScheduleId = null))
        }
    }

    suspend fun updateNote(item: CaptureItem, note: String?) {
        captureDao.update(item.toEntity().copy(note = note))
    }

    suspend fun delete(item: CaptureItem) {
        captureDao.delete(item.toEntity())
        imageStore.delete(item.imagePath)
    }

    /** SCHEDULE 캡처를 실제 일정으로 등록하고 연결한다. */
    suspend fun registerAsSchedule(item: CaptureItem, seed: ParsedDraftSeed): Long? {
        val date = seed.date ?: return null
        val scheduleId = scheduleRepository.create(
            Schedule(
                title = seed.title?.ifBlank { item.title } ?: item.title,
                date = date,
                startMinuteOfDay = seed.startMinuteOfDay,
                location = seed.location,
                category = seed.category,
                isImportant = seed.isImportant,
                source = seed.source,
                reminderOffsetMin = 10,
            ),
        )
        captureDao.update(item.toEntity().copy(sourceScheduleId = scheduleId))
        return scheduleId
    }
}
