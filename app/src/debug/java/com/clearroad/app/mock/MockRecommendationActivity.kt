package com.clearroad.app.mock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearroad.app.RecommendationSurface
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.ui.model.RecommendationSurfaceUiModel
import com.clearroad.app.ui.theme.ClearRoad2Theme

/**
 * Debug-only UI composition smoke for [RecommendationSurface].
 * Static presentation fixture — not a real route or provider result.
 */
class MockRecommendationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClearRoad2Theme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    RecommendationSurface(model = mockRecommendationSurfaceUiModel())
                }
            }
        }
    }
}

private fun mockRecommendationSurfaceUiModel(): RecommendationSurfaceUiModel =
    RecommendationSurfaceUiModel(
        ready = true,
        loading = false,
        routeName = "Fixture Route · Debug Smoke",
        travelTime = "32 min",
        decisionSummary = "Best Decision Right Now",
        decisionLabel = "DECISION",
        recommendationBadge = "⚡ Recommended",
        recommendationReason = "Debug fixture — composition smoke only",
        comparativeEvidenceLines = listOf(
            "Compared for presentation:",
            "Time · static line",
            "Salik · static line",
        ),
        narrative = "Static narrative for on-device RecommendationSurface rendering check.",
        mode = PreferenceMode.FASTEST,
    )
