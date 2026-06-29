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
import com.clearroad.app.intelligence.RouteIntelligencePresentation
import com.clearroad.app.intelligence.RouteIntelligenceReportLogger
import com.clearroad.app.intelligence.RouteIntelligenceService
import com.clearroad.app.intelligence.RouteIntelligenceAssembly
import com.clearroad.app.ui.model.RouteDetailsUiModel
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
    cardBorder: BorderStroke,
) {
    if (request == null) return

    var intelligence by remember(request) {
        mutableStateOf(RouteIntelligenceUiModel(loading = true))
    }

    LaunchedEffect(request, comparisonRequest) {
        intelligence = RouteIntelligenceUiModel(loading = true)
        intelligence =
            withContext(Dispatchers.IO) {
                RouteIntelligencePresentation.load(request)
            }
        comparisonRequest?.let { comparison ->
            val report =
                RouteIntelligenceService.default().comparisonReportFor(
                    routes = RouteIntelligenceAssembly.toRawRoutes(comparison),
                    marshioSelectedRouteIndex = comparison.marshioSelectedRouteIndex,
                    googleDefaultRouteIndex = comparison.googleDefaultRouteIndex,
                )
            RouteIntelligenceReportLogger.log(report)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    RouteIntelligenceCard(
        intelligence = intelligence,
        cardBorder = cardBorder,
    )
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
                    intelligence.marshioRoute?.let { row ->
                        RouteIntelligenceRow(row)
                    }
                    intelligence.alternativeRoute?.let { row ->
                        if (intelligence.marshioRoute != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        RouteIntelligenceRow(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteIntelligenceRow(row: RouteIntelligenceRouteRowUiModel) {
    Text(
        text = row.label,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = ClearRoadColors.RoadGrey,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = row.routeName,
        style = MaterialTheme.typography.bodySmall,
        color = ClearRoadColors.RoadGreyMuted,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = "🚦 ${row.trafficSignalsLine}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "⭕ ${row.roundaboutsLine}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "🛣 ${row.mainRoadLine}", style = MaterialTheme.typography.bodyMedium)
    Text(
        text = "😌 ${row.complexityLine}",
        style = MaterialTheme.typography.bodyMedium,
        color = ClearRoadColors.RoadGrey,
    )
}
