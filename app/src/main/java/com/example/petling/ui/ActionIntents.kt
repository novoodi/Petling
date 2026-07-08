package com.example.petling.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** 링크·지도 등 '비서 행동'을 여는 헬퍼. 실패해도 앱은 죽지 않는다. */
object ActionIntents {

    fun openUrl(context: Context, url: String) {
        val fixed = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(fixed)), "열 수 있는 앱이 없어요")
    }

    /** 장소명으로 지도 검색을 연다(설치된 지도 앱 → 없으면 웹). */
    fun openMap(context: Context, query: String) {
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
        if (geo.resolveActivity(context.packageManager) != null) {
            launch(context, geo, "지도 앱을 열 수 없어요")
        } else {
            openUrl(context, "https://map.naver.com/p/search/${Uri.encode(query)}")
        }
    }

    private fun launch(context: Context, intent: Intent, failMsg: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, failMsg, Toast.LENGTH_SHORT).show() }
    }
}
