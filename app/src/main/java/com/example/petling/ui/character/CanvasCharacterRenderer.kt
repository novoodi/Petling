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
import androidx.compose.runtime.remember
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
 * 캐릭터가 항상 "살아있게" 움직이도록 여러 트랙을 합성한다:
 * - IDLE: 숨쉬기 부유 + 눈 깜빡임 + 살랑 기울기 + 폴짝 더블 홉(+착지 먼지) + 귀 쫑긋 + 꼬리 살랑
 * - BOUNCE: 신나는 연속 통통 바운스(캡처 정리·완료 리액션), 꼬리 동기
 */
class CanvasCharacterRenderer : CharacterRenderer {

    @Composable
    override fun Render(spec: CharacterSpec, modifier: Modifier) {
        val palette = ModoriPalette.from(spec.colorHue, spec.species)
        val traits = remember(spec.seed) { IndividualTraits.from(spec.seed) }
        val transition = rememberInfiniteTransition(label = "creature")

        // 숨쉬기(위아래 부유 + 몸 팽창)
        val bob by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
            label = "bob",
        )

        // 깜빡임
        val blink by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4200
                    0f at 0; 0f at 3800; 1f at 3950; 0f at 4100
                },
            ),
            label = "blink",
        )

        // 살랑 기울기
        val sway by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3400), RepeatMode.Reverse),
            label = "sway",
        )

        // 폴짝 더블 홉(0..1 점프 높이)
        val hop by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 6400
                    0f at 0; 0f at 4700; 1f at 4880; 0f at 5060; 0.7f at 5220; 0f at 5400; 0f at 6400
                },
            ),
            label = "hop",
        )

        // 착지 먼지(홉 착지 순간 짧게)
        val dust by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 6400
                    0f at 0; 0f at 5050; 1f at 5120; 0.4f at 5320; 0f at 5600; 0f at 6400
                },
            ),
            label = "dust",
        )

        // 귀 쫑긋(홉과 어긋난 주기)
        val earTwitch by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 7300
                    0f at 0; 0f at 5600; 1f at 5680; 0f at 5820; 0.6f at 5960; 0f at 6100; 0f at 7300
                },
            ),
            label = "earTwitch",
        )

        // 꼬리 살랑(연속 위상)
        val tailWag by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "tailWag",
        )

        // 하트/zzz 상승 위상
        val effectPhase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "effectPhase",
        )

        // 신나는 연속 바운스(BOUNCE)
        val bouncePhase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Restart),
            label = "bounce",
        )

        // 보행 위상(WALK): 정면 홉 사이클. 주기는 종별 홉 스타일이 결정한다.
        val walkPhase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(hopStyleFor(spec.species).periodMs, easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "walk",
        )

        // 깊고 느린 숨(SLEEP)
        val sleepBreathe by transition.animateFloat(
            initialValue = -1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3800), RepeatMode.Reverse),
            label = "sleepBreathe",
        )

        // 냠냠(EAT)
        val eatPhase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Restart),
            label = "eat",
        )

        // 그루밍(GROOM)
        val groomPhase by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "groom",
        )

        // 쪼기(PECK): 머무르다 콕 숙였다 들기
        val peckPhase by transition.animateFloat(
            initialValue = 0f, targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 620
                    0f at 0; 0f at 200; 1f at 320; 0f at 480; 0f at 620
                },
            ),
            label = "peck",
        )

        Canvas(modifier = modifier) {
            val base = size.minDimension
            val idle = spec.animation == CharacterAnimation.IDLE
            val bouncing = spec.animation == CharacterAnimation.BOUNCE
            val walking = spec.animation == CharacterAnimation.WALK
            val sleeping = spec.animation == CharacterAnimation.SLEEP
            val eating = spec.animation == CharacterAnimation.EAT
            val grooming = spec.animation == CharacterAnimation.GROOM
            val pecking = spec.animation == CharacterAnimation.PECK

            var dy = 0f
            var scaleX = 1f
            var scaleY = 1f
            var tilt = 0f
            var motion = CreatureMotion.STATIC
            var eyeBlink = blink

            if (walking) {
                // 정면 홉 보행: 정면 유지 + 진행 방향 기울임 + 통통 홉(squash/stretch) + 착지 먼지.
                // 좌/우 방향은 호출부 graphicsLayer scaleX 반전이 처리한다.
                val pose = hopPose(hopStyleFor(spec.species), walkPhase)
                dy = pose.dyNorm * base
                tilt = pose.tiltDeg
                scaleX = pose.scaleX
                scaleY = pose.scaleY
                motion = CreatureMotion(
                    tailWag = walkPhase,
                    breathe = bob,
                    effectPhase = effectPhase,
                    dust = pose.dust,
                )
            } else if (sleeping) {
                dy = sleepBreathe * base * 0.022f
                tilt = 2.5f
                scaleY = 1f + sleepBreathe * 0.02f
                eyeBlink = maxOf(blink, 0.9f) // 완전 감김
                motion = CreatureMotion(
                    breathe = sleepBreathe,
                    effectPhase = effectPhase,
                )
            } else if (eating) {
                // 앞으로 숙이고 냠냠 씹는 펄스
                val chew = abs(sin(eatPhase * 2 * PI)).toFloat()
                tilt = 8f + sin(eatPhase * 2 * PI).toFloat() * 4f
                dy = chew * base * 0.015f
                scaleY = 1f - chew * 0.05f
                motion = CreatureMotion(breathe = bob, effectPhase = effectPhase, tailWag = eatPhase)
            } else if (grooming) {
                // 옆으로 기울여 핥는 헤드밥, 지그시 감은 눈
                tilt = 12f
                dy = sin(groomPhase * 2 * PI).toFloat() * base * 0.012f
                eyeBlink = maxOf(blink, 0.55f)
                motion = CreatureMotion(breathe = bob, effectPhase = effectPhase, tailWag = groomPhase)
            } else if (pecking) {
                // 바닥 콕콕 쪼기(몸 전체 숙임 근사) + 톡톡 먼지
                tilt = peckPhase * 20f
                dy = peckPhase * base * 0.03f
                motion = CreatureMotion(breathe = bob, effectPhase = effectPhase, dust = peckPhase * 0.3f)
            } else if (idle) {
                dy = bob * base * 0.015f
                tilt = sway * 2.2f
                if (hop > 0f) {
                    dy -= hop * base * 0.085f
                    scaleY = 1f + hop * 0.06f
                    scaleX = 1f - hop * 0.04f
                }
                motion = CreatureMotion(
                    earTwitch = earTwitch,
                    tailWag = tailWag,
                    effectPhase = effectPhase,
                    dust = dust,
                    breathe = bob,
                )
            } else if (bouncing) {
                val h = abs(sin(bouncePhase * PI)).toFloat()
                dy = -h * base * 0.10f
                val squash = (1f - h).coerceIn(0f, 1f)
                scaleX = 1f + squash * 0.08f
                scaleY = 1f - squash * 0.10f
                tilt = sin(bouncePhase * 2 * PI).toFloat() * 1.5f
                motion = CreatureMotion(
                    tailWag = bouncePhase,   // 점프 리듬에 꼬리 동기
                    effectPhase = effectPhase,
                    breathe = h,
                )
            }

            val pivot = Offset(size.width / 2f, size.height * 0.88f)
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
                            blink = eyeBlink,
                            motion = motion,
                            traits = traits,
                        )
                    }
                }
            }
        }
    }
}
