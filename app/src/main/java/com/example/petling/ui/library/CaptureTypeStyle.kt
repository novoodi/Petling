package com.example.petling.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CaptureType

/** 기반 종류별 (글자색, 배경색). 파스텔 배지. 커스텀 카테고리도 baseType 색을 쓴다. */
fun categoryColors(baseType: CaptureType): Pair<Color, Color> = when (baseType) {
    CaptureType.SCHEDULE -> Color(0xFFC4632D) to Color(0xFFFDF3EC)
    CaptureType.STUDY -> Color(0xFF1F4EF5) to Color(0xFFEBF0FF)
    CaptureType.CHAT -> Color(0xFF16A34A) to Color(0xFFF0FFF4)
    CaptureType.LINK -> Color(0xFF7C3AED) to Color(0xFFF5F3FF)
    CaptureType.PLACE -> Color(0xFF0E7490) to Color(0xFFECFEFF)
    CaptureType.SHOPPING -> Color(0xFFDB2777) to Color(0xFFFDF2F8)
    CaptureType.MEMORY -> Color(0xFF7A756B) to Color(0xFFF4F1EB)
}

/** 카테고리 배지. category가 null(삭제된 커스텀 등)이면 중립 표시. */
@Composable
fun CategoryBadge(category: Category?, modifier: Modifier = Modifier) {
    val (fg, bg) = categoryColors(category?.baseType ?: CaptureType.MEMORY)
    Text(
        text = category?.display ?: "기타",
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
