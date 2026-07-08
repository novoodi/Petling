package com.example.petling.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.ui.components.CategoryBadge
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.components.formatTime
import com.example.petling.ui.theme.Dimens

/** 캡처/음성 공용 파싱 draft 카드. */
@Composable
fun DraftCard(
    draft: ParsedDraftSeed,
    onQuickSave: () -> Unit,
    onReview: () -> Unit,
) {
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.title ?: "제목 없음", style = MaterialTheme.typography.titleSmall)
                val dateStr = draft.date?.toString() ?: "날짜 미정"
                Text(
                    "$dateStr · ${formatTime(draft.startMinuteOfDay)}" +
                        (draft.location?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            draft.category?.let { CategoryBadge(it) }
        }
        Spacer(Modifier.height(Dimens.Space3))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
            PetlingButton(
                "확인·수정",
                onClick = onReview,
                style = PetlingButtonStyle.Secondary,
                fillWidth = false,
                modifier = Modifier.weight(1f),
            )
            PetlingButton(
                "바로 등록",
                onClick = onQuickSave,
                enabled = draft.date != null,
                fillWidth = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
