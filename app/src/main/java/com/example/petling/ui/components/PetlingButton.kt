package com.example.petling.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.petling.ui.theme.BorderStrong
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.Motion
import com.example.petling.ui.theme.TextOnBrand
import com.example.petling.ui.theme.TextPrimary

enum class PetlingButtonStyle { Primary, Secondary, Ghost }

/**
 * 디자인 시스템 버튼. 높이 53dp(Primary CTA), press scale(0.97) 120ms.
 */
@Composable
fun PetlingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PetlingButtonStyle = PetlingButtonStyle.Primary,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PressScale else 1f,
        animationSpec = tween(Motion.Press),
        label = "btnScale",
    )

    val bg = if (style == PetlingButtonStyle.Primary) Brand500 else Color.Transparent
    val fg = if (style == PetlingButtonStyle.Primary) TextOnBrand else TextPrimary

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(Dimens.ButtonLg)
            .scale(scale)
            .alpha(if (enabled) 1f else Motion.DisabledAlpha)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(bg)
            .then(
                if (style == PetlingButtonStyle.Secondary) {
                    Modifier.border(1.dp, BorderStrong, RoundedCornerShape(Dimens.RadiusMd))
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
