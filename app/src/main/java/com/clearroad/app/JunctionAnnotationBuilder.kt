package com.clearroad.app

internal object JunctionAnnotationBuilder {
    fun build(route: RealRouteDebugData): List<RouteJunctionAnnotation> =
        buildFromSteps(route.googleSteps)

    fun buildFromSteps(steps: List<DirectionsStepRecord>): List<RouteJunctionAnnotation> {
        if (steps.isEmpty()) return emptyList()

        val candidates =
            steps.mapNotNull { step ->
                val type = JunctionAnnotationClassifier.classify(step) ?: return@mapNotNull null
                val position = step.startLocation ?: return@mapNotNull null
                val instruction =
                    JunctionAnnotationClassifier.shortLabel(
                        type = type,
                        instruction = JunctionAnnotationClassifier.stripHtml(step.htmlInstructions),
                    )
                ScoredJunction(
                    type = type,
                    position = position,
                    instruction = instruction,
                    score = JunctionAnnotationClassifier.complexityScore(type),
                )
            }

        if (candidates.isEmpty()) return emptyList()

        return candidates
            .sortedWith(
                compareByDescending<ScoredJunction> { it.score }
                    .thenBy { it.instruction.length },
            )
            .distinctBy { "${it.position.latitude}_${it.position.longitude}_${it.type}" }
            .take(JunctionAnnotationClassifier.MAX_ANNOTATIONS)
            .map { scored ->
                RouteJunctionAnnotation(
                    type = scored.type,
                    position = scored.position,
                    instruction = scored.instruction,
                )
            }
    }

    private data class ScoredJunction(
        val type: JunctionType,
        val position: com.google.android.gms.maps.model.LatLng,
        val instruction: String,
        val score: Int,
    )
}
