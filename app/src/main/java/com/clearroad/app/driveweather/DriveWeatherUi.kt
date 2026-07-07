package com.clearroad.app.ui.driveweather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class DriveMoodPalette(
    val ambientTop: Color,
    val ambientBottom: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val heroAccent: Color,
    val heroOnSurface: Color,
    val routeLine: Color,
)

@Composable
private fun rememberDriveMoodPalette(mood: DriveMood): DriveMoodPalette =
    remember(mood) {
        when (mood) {
            DriveMood.CALM ->
                DriveMoodPalette(
                    ambientTop = Color(0xFFEAF7EE),
                    ambientBottom = Color(0xFFF6FBF7),
                    heroGradientStart = Color(0xFFDDF3E4),
                    heroGradientEnd = Color(0xFFF2FBF5),
                    heroAccent = Color(0xFF1B7A3D),
                    heroOnSurface = Color(0xFF1A2E1F),
                    routeLine = Color(0xFF4CAF50),
                )
            DriveMood.BUSY ->
                DriveMoodPalette(
                    ambientTop = Color(0xFFFFF0E3),
                    ambientBottom = Color(0xFFFFFAF4),
                    heroGradientStart = Color(0xFFFFE0C2),
                    heroGradientEnd = Color(0xFFFFF6EC),
                    heroAccent = Color(0xFFE65100),
                    heroOnSurface = Color(0xFF3A2418),
                    routeLine = Color(0xFFFF9800),
                )
            DriveMood.HEAVY ->
                DriveMoodPalette(
                    ambientTop = Color(0xFFFDECEC),
                    ambientBottom = Color(0xFFFFF7F7),
                    heroGradientStart = Color(0xFFF8D4D4),
                    heroGradientEnd = Color(0xFFFFF0F0),
                    heroAccent = Color(0xFFC62828),
                    heroOnSurface = Color(0xFF3A1818),
                    routeLine = Color(0xFFE53935),
                )
        }
    }

@Composable
internal fun DriveMoodBackground(
    mood: DriveMood,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = rememberDriveMoodPalette(mood)
    Box(
        modifier =
            modifier.background(
                Brush.verticalGradient(
                    colors = listOf(palette.ambientTop, palette.ambientBottom),
                ),
            ),
    ) {
        content()
    }
}

@Composable
internal fun DriveWeatherSection(
    model: DriveWeatherUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DriveWeatherHeroCard(
            hero = model.hero,
            animationKey = model.animationKey,
        )
        LiveUpdateLabel(
            text = model.liveUpdateLabel,
            modifier = Modifier.padding(start = 4.dp),
        )
        DriveWeatherChipsSection(
            animationKey = model.animationKey,
            chips = model.chips,
        )
        DriveWeatherStoryExpandable(
            animationKey = model.animationKey,
            storySteps = model.storySteps,
        )
    }
}

@Composable
internal fun DriveWeatherHeroCard(
    hero: DriveWeatherHero,
    animationKey: Any,
    modifier: Modifier = Modifier,
) {
    val palette = rememberDriveMoodPalette(hero.mood)
    var visible by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        visible = false
        delay(40)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(animationSpec = tween(280)) +
                slideInVertically(animationSpec = tween(280)) { fullHeight -> fullHeight / 12 },
    ) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palette.heroGradientStart, palette.heroGradientEnd),
                        ),
                    ),
        ) {
            val signature = hero.resolvedDriveSignature()
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 26.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(text = hero.moodEmoji, fontSize = 26.sp)
                        Text(
                            text = hero.moodCaption,
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = palette.heroAccent,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = hero.adviceLine1,
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 32.sp,
                                ),
                            color = palette.heroOnSurface,
                        )
                        Text(
                            text = hero.adviceLine2,
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 32.sp,
                                ),
                            color = palette.heroOnSurface,
                        )
                    }
                    HeroMetricsRow(hero = hero, palette = palette)
                    DecisionConfidenceLine(
                        confidence = hero.decisionConfidence,
                        color = palette.heroOnSurface.copy(alpha = 0.72f),
                    )
                }
                DriveSignatureBand(
                    signature = signature,
                    animationKey = animationKey,
                    lineColor = palette.routeLine,
                    startLabel = hero.routeStartLabel,
                    endLabel = hero.routeEndLabel,
                    labelColor = palette.heroOnSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun DecisionConfidenceLine(
    confidence: DecisionConfidence,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF374151),
) {
    Text(
        text = confidence.displayLabel,
        modifier = modifier,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
        color = color,
    )
}

@Composable
internal fun DecisionStorySteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex
            Text(
                text = step,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                        lineHeight = 22.sp,
                    ),
                color = if (isLast) Color(0xFF111827) else Color(0xFF374151),
            )
            if (!isLast) {
                Text(
                    text = "↓",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 18.sp,
                        ),
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun LiveUpdateLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF6B7280).copy(alpha = 0.85f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriveWeatherChipsSection(
    animationKey: Any,
    chips: List<DriveWeatherChip>,
) {
    if (chips.isEmpty()) return

    var showSection by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        showSection = false
        delay(200)
        showSection = true
    }

    AnimatedVisibility(
        visible = showSection,
        enter =
            fadeIn(animationSpec = tween(260)) +
                slideInVertically(animationSpec = tween(260)) { fullHeight -> fullHeight / 16 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "What awaits you",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF374151),
                modifier = Modifier.padding(start = 4.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEachIndexed { index, chip ->
                    StaggeredDriveWeatherChip(
                        chip = chip,
                        animationKey = animationKey,
                        index = index,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredDriveWeatherChip(
    chip: DriveWeatherChip,
    animationKey: Any,
    index: Int,
) {
    var visible by remember(animationKey, index) { mutableStateOf(false) }
    LaunchedEffect(animationKey, index) {
        visible = false
        delay(240L + index * 45L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
    ) {
        val label = buildString {
            chip.emoji?.let { append("$it ") }
            append(chip.text)
        }
        Text(
            text = label,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.82f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF111827),
        )
    }
}

@Composable
private fun DriveWeatherStoryExpandable(
    animationKey: Any,
    storySteps: List<String>,
) {
    if (storySteps.isEmpty()) return

    var expanded by remember(animationKey) { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(if (expanded) "Hide details" else "Why this route?")
    }

    if (expanded) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.78f),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DecisionStorySteps(steps = storySteps)
            }
        }
    }
}

@Composable
private fun HeroMetricsRow(
    hero: DriveWeatherHero,
    palette: DriveMoodPalette,
) {
    val hasEta = hero.etaDeltaValue != null && hero.etaDeltaUnit != null
    val hasSavings = hero.savingsValue != null && hero.savingsUnit != null
    if (!hasEta && !hasSavings) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (hasEta) {
            HeroMetric(
                value = hero.etaDeltaValue.orEmpty(),
                unit = hero.etaDeltaUnit.orEmpty(),
                accent = palette.heroOnSurface,
                muted = palette.heroOnSurface.copy(alpha = 0.55f),
            )
        }
        if (hasSavings) {
            HeroMetric(
                value = hero.savingsValue.orEmpty(),
                unit = hero.savingsUnit.orEmpty(),
                accent = palette.heroAccent,
                muted = palette.heroAccent.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun HeroMetric(
    value: String,
    unit: String,
    accent: Color,
    muted: Color,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 34.sp,
                ),
            color = accent,
        )
        Text(
            text = unit,
            modifier = Modifier.padding(bottom = 3.dp),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = muted,
        )
    }
}
