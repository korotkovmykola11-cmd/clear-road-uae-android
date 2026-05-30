package com.clearroad.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.clearroad.app.ui.model.ChoiceWhyTipUiModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ChoiceWhyTipBlock(
    model: ChoiceWhyTipUiModel,
    modifier: Modifier = Modifier,
) {
    ChoiceWhyTipBlock(
        selectedDecisionTitle = model.choice,
        selectedDecisionWhy = model.why,
        selectedDecisionTip = model.tip,
        compact = model.compact,
        modifier = modifier,
    )
}

@Composable
internal fun ChoiceWhyTipBlock(
    selectedDecisionTitle: String,
    selectedDecisionWhy: String,
    selectedDecisionTip: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val recLabelStyle =
        if (compact) {
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp,
            )
        } else {
            MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp,
            )
        }
    val recBodyStyle =
        if (compact) {
            MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp)
        } else {
            MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
        }
    val recGapLabelToBody = if (compact) 1.dp else 3.dp
    val recGapBetweenSections = if (compact) 3.dp else 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 1.dp else 6.dp),
    ) {
        Text(
            text = "Choice",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = selectedDecisionTitle,
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(recGapBetweenSections))

        Text(
            text = "Why",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = selectedDecisionWhy,
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(recGapBetweenSections))

        Text(
            text = "Tip",
            style = recLabelStyle,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(recGapLabelToBody))
        Text(
            text = selectedDecisionTip,
            style = recBodyStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
