package com.example.petling.ui.character

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.translate
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec

/**
 * Compose Canvas 기반 캐릭터 렌더러(이번 구현).
 * idle 상태에서 숨쉬기 바운스와 눈 깜빡임을 재생한다.
 */
class CanvasCharacterRenderer : CharacterRenderer {

    @Composable
    override fun Render(spec: CharacterSpec, modifier: Modifier) {
        val palette = ModoriPalette.from(spec.colorHue)
        val transition = rememberInfiniteTransition(label = "modori")

        // 숨쉬기(위아래 부유)
        val breatheAmp = if (spec.animation == CharacterAnimation.IDLE) 1f else 0f
        val bob by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bob",
        )

        // 깜빡임: 대부분 열려 있다가 짧게 감음
        val blink by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4200
                    0f at 0
                    0f at 3800
                    1f at 3950
                    0f at 4100
                },
            ),
            label = "blink",
        )

        Canvas(modifier = modifier) {
            val dy = bob * breatheAmp * size.minDimension * 0.015f
            translate(top = dy) {
                drawCreature(
                    species = spec.species,
                    stage = spec.stage,
                    branch = spec.branch,
                    mood = spec.mood,
                    expression = spec.expression,
                    palette = palette,
                    eyeStyle = spec.eyeStyle,
                    blink = blink,
                )
            }
        }
    }
}
