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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Species
import com.example.petling.ui.character.CreatureMotion
import com.example.petling.ui.character.CreatureView
import com.example.petling.ui.character.ModoriPalette
import com.example.petling.ui.character.drawCreature

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

        Header("옆모습 보행 (성숙기, walkCycle 0.25)")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Species.entries.filter { it != Species.ACORN }.forEach { sp ->
                CellLabeled(sp.displayName, sp, GrowthStage.MATURE, null, Mood.CALM, Expression.NEUTRAL, sp.defaultHue, 0, 108, side = true, walk = 0.25f)
            }
        }

        Header("걷기 사이클 (강아지: 0 / 0.25 / 0.5 / 0.75)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0f, 0.25f, 0.5f, 0.75f).forEach { phi ->
                CellLabeled("$phi", Species.DOG, GrowthStage.MATURE, null, Mood.CALM, Expression.NEUTRAL, Species.DOG.defaultHue, 0, 108, side = true, walk = phi)
            }
        }

        Header("걷기 사이클 (토끼 홉: 0 / 0.2 / 0.4 / 0.7)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0f, 0.2f, 0.4f, 0.7f).forEach { phi ->
                CellLabeled("$phi", Species.RABBIT, GrowthStage.MATURE, null, Mood.CALM, Expression.NEUTRAL, Species.RABBIT.defaultHue, 0, 108, side = true, walk = phi)
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
    side: Boolean = false, walk: Float = 0f,
) {
    val palette = ModoriPalette.from(hue, sp)
    Canvas(modifier = Modifier.size(dp.dp).background(Color(0xFFFBF9F4))) {
        drawCreature(
            sp, st, br, mood, ex, palette, eye, blink = 0f,
            motion = CreatureMotion(walkCycle = walk),
            view = if (side) CreatureView.SIDE else CreatureView.FRONT,
        )
    }
}

@Composable
private fun CellLabeled(
    label: String, sp: Species, st: GrowthStage, br: Branch?, mood: Mood, ex: Expression, hue: Float, eye: Int, dp: Int,
    side: Boolean = false, walk: Float = 0f,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Cell(sp, st, br, mood, ex, hue, eye, dp, side, walk)
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
