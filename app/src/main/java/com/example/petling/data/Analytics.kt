package com.example.petling.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * 베타 측정용 이벤트(익명, 개인정보 없음). 보려는 숫자는 딱 세 개:
 * 7일 재방문, 1인당 기록 수, 촬영 없이 시세만 본 사람 수. 그래서 이벤트도 그만큼만 둔다.
 * 상품명·가격·매장명 같은 사용자 데이터는 절대 보내지 않는다.
 */
class Analytics(context: Context) {

    private val fa: FirebaseAnalytics? = runCatching { FirebaseAnalytics.getInstance(context.applicationContext) }.getOrNull()

    /** 화면 진입. [name]은 고정 문자열(price_home / market_search / market_product / settings 등). */
    fun screen(name: String) = log(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
    }

    /** 가격표 분석 시작(촬영 또는 앨범). */
    fun analyzeStarted(source: String) = log("analyze_start") { putString("source", source) }

    /** 기록 저장. 시장 카드가 붙었는지, 매장을 골랐는지만 보낸다. */
    fun recordSaved(hasMarket: Boolean, hasStore: Boolean) = log("record_saved") {
        putLong("has_market", if (hasMarket) 1 else 0)
        putLong("has_store", if (hasStore) 1 else 0)
    }

    /** 영수증 분석 시작 / 저장(건수만). */
    fun receiptStarted(source: String) = log("receipt_start") { putString("source", source) }
    fun receiptSaved(count: Int, parsedCount: Int) = log("receipt_saved") {
        putLong("saved", count.toLong())
        putLong("parsed", parsedCount.toLong())
    }

    /** 시세 검색 실행(검색어는 보내지 않고 결과 수만). */
    fun marketSearched(resultCount: Int) = log("market_search") { putLong("results", resultCount.toLong()) }

    /** 시세 상품 상세 진입(어디서 왔는지). */
    fun marketProductOpened(from: String) = log("market_product_open") { putString("from", from) }

    private inline fun log(event: String, params: Bundle.() -> Unit = {}) {
        fa?.logEvent(event, Bundle().apply(params))
    }
}
