package com.example.petling.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Species
import com.example.petling.ui.character.CreatureMotion
import com.example.petling.ui.character.IndividualTraits
import com.example.petling.ui.character.ModoriPalette
import com.example.petling.ui.character.drawCreature
import com.example.petling.ui.character.hopPose
import com.example.petling.ui.character.hopStyleFor

/**
 * 디버그 전용 캐릭터 갤러리(릴리스 미포함).
 * 실행: adb shell am start -n com.petling.app/com.example.petling.debug.CharacterGalleryActivity
 */
class CharacterGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Gallery() }
    }
}

private val STAGES = listOf(GrowthStage.JUVENILE, GrowthStage.GROWTH1, GrowthStage.GROWTH2, GrowthStage.MATURE)

@Composable
private fun Gallery() {
    Column(
        modifier = Modifier
            .background(Color(0xFFF3EFE7))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Header("성장 단계 (유생→성숙)")
        Species.entries.forEach { sp ->
            Label(sp.displayName)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STAGES.forEach { st ->
                    Cell(sp, st, null, Mood.CALM, Expression.NEUTRAL, sp.defaultHue, 0, 108)
                }
            }
        }

        Header("체급 비교 (성숙기, 동일 셀 — 발끝 정렬·판다 클리핑 검수)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Species.entries.forEach { sp ->
                CellLabeled(sp.displayName, sp, GrowthStage.MATURE, null, Mood.CALM, Expression.NEUTRAL, sp.defaultHue, 0, 120)
            }
        }

        Header("소형 종 아웃라인 가시성 (유생기·72dp)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Species.CHICK, Species.HAMSTER, Species.RABBIT, Species.CAT).forEach { sp ->
                CellLabeled(sp.displayName, sp, GrowthStage.JUVENILE, null, Mood.CALM, Expression.NEUTRAL, sp.defaultHue, 0, 72)
            }
        }

        Header("단색 실루엣 (성숙기 — 실루엣만으로 9종 구분 게이트)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Species.entries.forEach { sp ->
                SilhouetteCellLabeled(sp.displayName, sp, GrowthStage.MATURE)
            }
        }

        Header("개체 변이 (동일 종·성숙기 — seed별 미세차, 코트·실루엣 동일)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0L, 11L, 202L, 3003L, 40004L, 500005L).forEach { seed ->
                SeedCellLabeled(if (seed == 0L) "중립" else "seed $seed", Species.HAMSTER, seed)
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0L, 11L, 202L, 3003L, 40004L, 500005L).forEach { seed ->
                SeedCellLabeled(if (seed == 0L) "중립" else "seed $seed", Species.FOX, seed)
            }
        }

        Header("표정 6종 (고양이·성장기2)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Expression.entries.forEach { ex ->
                CellLabeled(ex.name, Species.CAT, GrowthStage.GROWTH2, null, moodFor(ex), ex, Species.CAT.defaultHue, 0, 100)
            }
        }

        Header("눈 모양 3종 (토끼)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..2).forEach { es ->
                CellLabeled("eye$es", Species.RABBIT, GrowthStage.GROWTH2, null, Mood.HAPPY, Expression.HAPPY, Species.RABBIT.defaultHue, es, 100)
            }
        }

        Header("진화 분기 (성숙기·여우)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Branch.STUDY, Branch.HOBBY, Branch.BALANCED).forEach { br ->
                CellLabeled(br.displayName, Species.FOX, GrowthStage.MATURE, br, Mood.CALM, Expression.NEUTRAL, Species.FOX.defaultHue, 0, 108)
            }
        }

        Header("색 슬라이더 스윕 (고양이)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0f, 45f, 90f, 140f, 200f, 260f, 320f).forEach { hue ->
                Cell(Species.CAT, GrowthStage.GROWTH2, null, Mood.CALM, Expression.NEUTRAL, hue, 0, 84)
            }
        }

        Header("작은 크기(72dp) 판독")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Species.entries.forEach { sp ->
                Cell(sp, GrowthStage.MATURE, null, Mood.CALM, Expression.HAPPY, sp.defaultHue, 0, 72)
            }
        }

        Header("정면 홉 보행 (성숙기, phi 0.25)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Species.entries.forEach { sp ->
                HopCellLabeled(sp.displayName, sp, GrowthStage.MATURE, sp.defaultHue, phi = 0.25f)
            }
        }

        Header("홉 사이클 (강아지: 0 / 0.25 / 0.5 / 0.75)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f).forEach { phi ->
                HopCellLabeled("$phi", Species.DOG, GrowthStage.MATURE, Species.DOG.defaultHue, phi)
            }
        }

        Header("홉 사이클 (토끼: 0 / 0.25 / 0.5 / 0.75)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f).forEach { phi ->
                HopCellLabeled("$phi", Species.RABBIT, GrowthStage.MATURE, Species.RABBIT.defaultHue, phi)
            }
        }

        Header("홉 사이클 (펭귄 뒤뚱: 0 / 0.25 / 0.5 / 0.75)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f).forEach { phi ->
                HopCellLabeled("$phi", Species.PENGUIN, GrowthStage.MATURE, Species.PENGUIN.defaultHue, phi)
            }
        }
    }
}

private fun moodFor(ex: Expression): Mood = when (ex) {
    Expression.HAPPY, Expression.EXCITED -> Mood.HAPPY
    Expression.SLEEPY -> Mood.TIRED
    else -> Mood.CALM
}

@Composable
private fun Cell(
    sp: Species, st: GrowthStage, br: Branch?, mood: Mood, ex: Expression, hue: Float, eye: Int, dp: Int,
) {
    val palette = ModoriPalette.from(hue, sp)
    Canvas(modifier = Modifier.size(dp.dp).background(Color(0xFFFBF9F4))) {
        drawCreature(sp, st, br, mood, ex, palette, eye, blink = 0f)
    }
}

@Composable
private fun CellLabeled(
    label: String, sp: Species, st: GrowthStage, br: Branch?, mood: Mood, ex: Expression, hue: Float, eye: Int, dp: Int,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Cell(sp, st, br, mood, ex, hue, eye, dp)
        Text(label, fontSize = 10.sp)
    }
}

/** seed별 개체 변이 셀 — 크기·볼·꼬리 미세차. 코트 색·실루엣 아키타입은 동일해야 한다. */
@Composable
private fun SeedCellLabeled(label: String, sp: Species, seed: Long, dp: Int = 108) {
    val palette = ModoriPalette.from(sp.defaultHue, sp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(dp.dp).background(Color(0xFFFBF9F4))) {
            drawCreature(
                sp, GrowthStage.MATURE, null, Mood.CALM, Expression.NEUTRAL, palette, 0, blink = 0f,
                traits = IndividualTraits.from(seed),
            )
        }
        Text(label, fontSize = 10.sp)
    }
}

/** 전 색을 아웃라인 단색으로 치환한 실루엣 셀 — 실루엣만으로 종이 구분되는지 판정. */
@Composable
private fun SilhouetteCellLabeled(label: String, sp: Species, st: GrowthStage, dp: Int = 120) {
    val o = ModoriPalette.OUTLINE
    val mono = ModoriPalette(
        body = o, bodyShadow = o, bodyHighlight = o, cap = o, capShadow = o, cheek = o,
        belly = o, earInner = o, marking = o, iris = o, nose = o, outline = o, accent = o, accentDark = o,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(dp.dp).background(Color(0xFFFBF9F4))) {
            drawCreature(sp, st, null, Mood.CALM, Expression.NEUTRAL, mono, 0, blink = 0f)
        }
        Text(label, fontSize = 10.sp)
    }
}

/** 정면 홉 포즈를 렌더러와 동일하게 적용한 정적 셀(phi 고정). */
@Composable
private fun HopCellLabeled(label: String, sp: Species, st: GrowthStage, hue: Float, phi: Float, dp: Int = 108) {
    val palette = ModoriPalette.from(hue, sp)
    val pose = hopPose(hopStyleFor(sp), phi)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(dp.dp).background(Color(0xFFFBF9F4))) {
            val base = size.minDimension
            val pivot = Offset(size.width / 2f, size.height * 0.88f)
            translate(top = pose.dyNorm * base) {
                rotate(degrees = pose.tiltDeg, pivot = Offset(size.width / 2f, size.height / 2f)) {
                    scale(scaleX = pose.scaleX, scaleY = pose.scaleY, pivot = pivot) {
                        drawCreature(
                            sp, st, null, Mood.CALM, Expression.NEUTRAL, palette, 0, blink = 0f,
                            motion = CreatureMotion(dust = pose.dust),
                        )
                    }
                }
            }
        }
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun Header(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Label(t: String) {
    Text(t, fontSize = 11.sp, color = Color(0xFF7A6A55))
}
