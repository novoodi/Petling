package com.example.petling.ui.components

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
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.ui.theme.CategoryAppointment
import com.example.petling.ui.theme.CategoryAppointmentBg
import com.example.petling.ui.theme.CategoryHobby
import com.example.petling.ui.theme.CategoryHobbyBg
import com.example.petling.ui.theme.CategoryRest
import com.example.petling.ui.theme.CategoryRestBg
import com.example.petling.ui.theme.CategoryStudy
import com.example.petling.ui.theme.CategoryStudyBg

fun categoryColors(category: ScheduleCategory): Pair<Color, Color> = when (category) {
    ScheduleCategory.STUDY -> CategoryStudy to CategoryStudyBg
    ScheduleCategory.APPOINTMENT -> CategoryAppointment to CategoryAppointmentBg
    ScheduleCategory.HOBBY -> CategoryHobby to CategoryHobbyBg
    ScheduleCategory.REST -> CategoryRest to CategoryRestBg
}

@Composable
fun CategoryBadge(category: ScheduleCategory, modifier: Modifier = Modifier) {
    val (fg, bg) = categoryColors(category)
    Text(
        text = category.displayName,
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
