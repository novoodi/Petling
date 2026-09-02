package com.example.petling.ui.market

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 추이 한 점: 날짜(epochDay)와 가격. */
data class TrendPoint(val epochDay: Long, val priceWon: Int)

/** 선 하나(예: 대형마트 중앙값). */
data class TrendSeries(val label: String, val color: Color, val points: List<TrendPoint>)

/**
 * 가격 추이 차트(Compose Canvas, 라이브러리 없음).
 * 시장 중앙값 선 + 내 기록 점을 같은 시간축에 겹쳐 "나는 시장보다 위/아래에서 사고 있었나"가 보이게 한다.
 * 점이 하나뿐이면 좌우로 7일 여백을 둬 점 하나라도 보이게 한다.
 */
@Composable
fun PriceTrendChart(
    series: List<TrendSeries>,
    myPoints: List<TrendPoint>,
    modifier: Modifier = Modifier,
    myLabel: String = "내 기록",
    myColor: Color = MaterialTheme.colorScheme.primary,
) {
    val all = series.flatMap { it.points } + myPoints
    if (all.isEmpty()) return

    val minDayRaw = all.minOf { it.epochDay }
    val maxDayRaw = all.maxOf { it.epochDay }
    val minDay = if (minDayRaw == maxDayRaw) minDayRaw - 7 else minDayRaw
    val maxDay = if (minDayRaw == maxDayRaw) maxDayRaw + 7 else maxDayRaw
    val minPriceRaw = all.minOf { it.priceWon }
    val maxPriceRaw = all.maxOf { it.priceWon }
    val pad = maxOf((maxPriceRaw - minPriceRaw) * 0.15, maxPriceRaw * 0.05, 100.0)
    val minPrice = (minPriceRaw - pad).coerceAtLeast(0.0)
    val maxPrice = maxPriceRaw + pad

    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier) {
        // 범례
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            series.forEach { s -> LegendItem(s.label, s.color) }
            if (myPoints.isNotEmpty()) LegendItem(myLabel, myColor, dot = true)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val leftPad = 64.dp.toPx()
            val bottomPad = 18.dp.toPx()
            val topPad = 6.dp.toPx()
            val w = size.width - leftPad - 8.dp.toPx()
            val h = size.height - bottomPad - topPad
            fun x(day: Long) = leftPad + (day - minDay).toFloat() / (maxDay - minDay).toFloat() * w
            fun y(price: Number) = topPad + (1f - ((price.toDouble() - minPrice) / (maxPrice - minPrice)).toFloat()) * h

            // 가로 격자 3줄 + 가격 라벨
            listOf(minPrice, (minPrice + maxPrice) / 2, maxPrice).forEach { p ->
                val yy = y(p)
                drawLine(gridColor, Offset(leftPad, yy), Offset(leftPad + w, yy), strokeWidth = 1f)
                val text = measurer.measure("%,d".format(p.toInt()), labelStyle)
                drawText(text, topLeft = Offset(leftPad - text.size.width - 6.dp.toPx(), yy - text.size.height / 2))
            }
            // 날짜 라벨(처음·끝)
            val fmt = DateTimeFormatter.ofPattern("M/d")
            val first = measurer.measure(LocalDate.ofEpochDay(minDayRaw).format(fmt), labelStyle)
            val last = measurer.measure(LocalDate.ofEpochDay(maxDayRaw).format(fmt), labelStyle)
            drawText(first, topLeft = Offset(x(minDayRaw) - first.size.width / 2, size.height - first.size.height))
            if (maxDayRaw != minDayRaw) {
                drawText(last, topLeft = Offset(x(maxDayRaw) - last.size.width / 2, size.height - last.size.height))
            }

            // 시장 선
            series.forEach { s ->
                val pts = s.points.sortedBy { it.epochDay }
                if (pts.size >= 2) {
                    val path = Path()
                    pts.forEachIndexed { i, p ->
                        if (i == 0) path.moveTo(x(p.epochDay), y(p.priceWon)) else path.lineTo(x(p.epochDay), y(p.priceWon))
                    }
                    drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                pts.forEach { p -> drawCircle(s.color, radius = 3.dp.toPx(), center = Offset(x(p.epochDay), y(p.priceWon))) }
            }
            // 내 기록 점(테두리 있는 큰 점)
            myPoints.forEach { p ->
                val c = Offset(x(p.epochDay), y(p.priceWon))
                drawCircle(Color.White, radius = 6.dp.toPx(), center = c)
                drawCircle(myColor, radius = 6.dp.toPx(), center = c, style = Stroke(width = 2.5.dp.toPx()))
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, dot: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dot) {
            Box(Modifier.size(10.dp).background(Color.White, CircleShape)) {
                Box(Modifier.size(10.dp).background(color.copy(alpha = 0.35f), CircleShape))
            }
        } else {
            Box(Modifier.width(14.dp).height(3.dp).background(color))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
