package com.example.petling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.petling.ui.theme.BorderDefault
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceCard

/**
 * 디자인 시스템 카드: 흰 배경 + 얕은 그림자 + radius 14dp + 1dp 옅은 테두리.
 */
@Composable
fun PetlingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(Dimens.RadiusLg),
                spotColor = androidx.compose.ui.graphics.Color(0x141A1B1E),
                ambientColor = androidx.compose.ui.graphics.Color(0x0F1A1B1E),
            )
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(SurfaceCard)
            .border(1.dp, BorderDefault, RoundedCornerShape(Dimens.RadiusLg))
            .padding(Dimens.Space4),
        content = content,
    )
}
