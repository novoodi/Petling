package com.example.petling.ui.character

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Compose Canvas 기반 캐릭터 렌더러.
 * 다마고치 감성을 위해 캐릭터가 항상 "살아있게" 움직인다:
 * - IDLE: 숨쉬기 부유 + 눈 깜빡임 + 살랑 기울기 + 이따금 폴짝 더블 홉(스쿼시&스트레치)
 * - BOUNCE: 신나는 연속 통통 바운스(캡처 정리·완료 리액션)
 */
class CanvasCharacterRenderer : CharacterRenderer {

    @Composable
    override fun Render(spec: CharacterSpec, modifier: Modifier) {
        val palette = ModoriPalette.from(spec.colorHue)
        val transition = rememberInfiniteTransition(label = "creature")

        // 숨쉬기(위아래 부유) — idle 기본 생명감
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

        // 살랑 기울기(idle): 좌우로 아주 천천히 갸웃갸웃
        val sway by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3400),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sway",
        )

        // 이따금 폴짝 더블 홉(idle): 6.4초 주기로 통·통 두 번 뛴다. 0..1 = 점프 높이
        val hop by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 6400
                    0f at 0
                    0f at 4700
                    1f at 4880
                    0f at 5060
                    0.7f at 5220
                    0f at 5400
                    0f at 6400
                },
            ),
            label = "hop",
        )

        // 신나는 연속 바운스(BOUNCE): 0..1 재시작 주기
        val bouncePhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(560, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "bounce",
        )

        Canvas(modifier = modifier) {
            val base = size.minDimension
            val idle = spec.animation == CharacterAnimation.IDLE
            val bouncing = spec.animation == CharacterAnimation.BOUNCE

            // 이동/변형량 계산
            var dy = 0f
            var scaleX = 1f
            var scaleY = 1f
            var tilt = 0f

            if (idle) {
                dy = bob * base * 0.015f
                tilt = sway * 2.2f
                if (hop > 0f) {
                    // 점프: 위로 뜨면서 살짝 늘어나고, 착지 순간 눌린다
                    dy -= hop * base * 0.085f
                    scaleY = 1f + hop * 0.06f
                    scaleX = 1f - hop * 0.04f
                }
            } else if (bouncing) {
                // sin 곡선으로 매끄럽게 튀어오르고, 바닥 근처에서 스쿼시
                val h = abs(sin(bouncePhase * PI)).toFloat()
                dy = -h * base * 0.10f
                val squash = (1f - h).coerceIn(0f, 1f)
                scaleX = 1f + squash * 0.08f
                scaleY = 1f - squash * 0.10f
                tilt = sin(bouncePhase * 2 * PI).toFloat() * 1.5f
            }

            val pivot = Offset(size.width / 2f, size.height * 0.88f) // 발밑 기준 스쿼시
            translate(top = dy) {
                rotate(degrees = tilt, pivot = Offset(size.width / 2f, size.height / 2f)) {
                    scale(scaleX = scaleX, scaleY = scaleY, pivot = pivot) {
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
    }
}
