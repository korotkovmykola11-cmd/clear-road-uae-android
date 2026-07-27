package com.clearroad.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearroad.app.intelligence.RouteIntelligencePresentation
import com.clearroad.app.intelligence.RouteIntelligenceReportLogger
import com.clearroad.app.ui.model.RouteIntelligenceComparisonRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRequestUiModel
import com.clearroad.app.ui.model.RouteIntelligenceRouteRowUiModel
import com.clearroad.app.ui.model.RouteIntelligenceUiModel
import com.clearroad.app.ui.theme.ClearRoadColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun RouteDetailsIntelligenceSection(
    request: RouteIntelligenceRequestUiModel?,
    comparisonRequest: RouteIntelligenceComparisonRequestUiModel?,
    showSection: Boolean,
    unavailableReason: String?,
    cardBorder: BorderStroke,
) {
    if (!showSection) return

    if (request == null) {
        Spacer(modifier = Modifier.height(12.dp))
        RouteIntelligenceUnavailableCard(
            message = unavailableReason ?: "Route Intelligence unavailable",
            cardBorder = cardBorder,
        )
        return
    }

    var intelligence by remember(request) {
        mutableStateOf(RouteIntelligenceUiModel(loading = true))
    }

    LaunchedEffect(request, comparisonRequest) {
        intelligence = RouteIntelligenceUiModel(loading = true)
        val loadResult =
            withContext(Dispatchers.IO) {
                RouteIntelligencePresentation.load(
                    request = request,
                    comparisonRequest = comparisonRequest,
                )
            }
        intelligence = loadResult.uiModel
        RouteIntelligenceReportLogger.log(
            RouteIntelligencePresentation.comparisonReportFromLoad(loadResult),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    RouteIntelligenceCard(
        intelligence = intelligence,
        cardBorder = cardBorder,
    )
}

@Composable
private fun RouteIntelligenceUnavailableCard(
    message: String,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Route Intelligence",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ClearRoadColors.RoadGreyMuted,
            )
        }
    }
}

@Composable
private fun RouteIntelligenceCard(
    intelligence: RouteIntelligenceUiModel,
    cardBorder: BorderStroke,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClearRoadColors.RouteCardSurfaceMuted,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Route Intelligence",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ClearRoadColors.RoadGreyMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                intelligence.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(vertical = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                !intelligence.available -> {
                    Text(
                        text = intelligence.unavailableMessage.ifBlank {
                            RouteIntelligencePresentation.UNAVAILABLE_MESSAGE
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ClearRoadColors.RoadGreyMuted,
                    )
                }
                else -> {
                    HorizontalDivider(color = ClearRoadColors.RoadGreyMuted.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (intelligence.summaryLine.isNotBlank()) {
                        Text(
                            text = intelligence.summaryLine,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                            ),
                            color = ClearRoadColors.RoadGrey,
                        )
                    }
                    if (intelligence.explanationLine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intelligence.explanationLine,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = ClearRoadColors.RoadGreyMuted,
                        )
                    }
                    intelligence.marshioRoute?.let { row ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = ClearRoadColors.RoadGreyMuted.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))
                        RouteIntelligenceDetailsBlock(row = row)
                    }
                    intelligence.alternativeRoute?.let { row ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = row.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = ClearRoadColors.RoadGreyMuted,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RouteIntelligenceDetailsBlock(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteIntelligenceDetailsBlock(row: RouteIntelligenceRouteRowUiModel) {
    Text(
        text = "Details",
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        ),
        color = ClearRoadColors.RoadGreyMuted.copy(alpha = 0.85f),
    )
    Spacer(modifier = Modifier.height(4.dp))
    RouteIntelligenceDetailLine("🚦 ${row.trafficSignalsLine}")
    RouteIntelligenceDetailLine("⭕ ${row.roundaboutsLine}")
    RouteIntelligenceDetailLine("🛣 ${row.mainRoadLine}")
    RouteIntelligenceDetailLine("😌 ${row.complexityLine}")
}

@Composable
private fun RouteIntelligenceDetailLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
        ),
        color = ClearRoadColors.RoadGreyMuted.copy(alpha = 0.9f),
    )
}
