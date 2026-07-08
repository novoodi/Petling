package com.example.petling.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.ui.theme.Dimens
import kotlin.math.roundToInt

/** 스프라이트 높이(마당 하단 기준). RoamingYard의 SPRITE와 일치. */
private val PET_SPRITE = 140.dp

/**
 * 앱 전역 마당 캐릭터. 하단 띠를 혼자 배회하고(모든 탭 공유), 홈에서는 머리 위에 말풍선을 띄운다.
 * 터치는 캐릭터·말풍선 위에서만 소비되고 나머지는 뒤 콘텐츠로 통과한다.
 */
@Composable
fun AppPet(
    baseSpec: CharacterSpec,
    affection: Int,
    state: YardState,
    showBubble: Boolean,
    greeting: String,
    onOpenCharacter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(modifier = modifier.fillMaxSize()) {
        // 말풍선(홈에서만) — 캐릭터 머리 위에 떠서 x를 따라 이동
        if (showBubble && greeting.isNotBlank()) {
            var bubbleW by remember { mutableIntStateOf(0) }
            val spriteTopPx = with(density) { PET_SPRITE.toPx() }
            val gapPx = with(density) { 4.dp.toPx() }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val maxXpx = with(density) { maxWidth.toPx() }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset {
                            val cx = state.centerX() - bubbleW / 2f
                            IntOffset(
                                cx.coerceIn(0f, (maxXpx - bubbleW).coerceAtLeast(0f)).roundToInt(),
                                -(spriteTopPx + gapPx).roundToInt(),
                            )
                        }
                        .onSizeChanged { bubbleW = it.width },
                ) {
                    PetBubble(greeting, onClick = onOpenCharacter)
                }
            }
        }

        // 마당(하단 배회)
        RoamingYard(baseSpec, affection, state, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PetBubble(text: String, onClick: () -> Unit) {
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(Dimens.RadiusLg))
                .background(bubbleColor)
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
        ) {
            Crossfade(targetState = text, label = "petSpeech") { t ->
                Text(
                    t,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // 캐릭터를 향해 아래로 향한 꼬리
        Canvas(modifier = Modifier.size(width = 18.dp, height = 9.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, bubbleColor)
        }
    }
}
