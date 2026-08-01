package com.clearroad.app



import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalDensity

import com.google.android.gms.maps.model.JointType

import com.google.android.gms.maps.model.LatLng

import com.google.android.gms.maps.model.RoundCap

import com.google.maps.android.compose.GoogleMapComposable

import com.google.maps.android.compose.Polyline



private val PreviewPolylineRoundCap = RoundCap()

private val PreviewPolylineJoint = JointType.ROUND



@Composable

@GoogleMapComposable

internal fun RenderTrafficSegments(

    segments: List<TrafficSegment>,

    density: Float,

    zIndexBase: Float = TrafficPolylineStyle.PreviewMainRouteHaloZIndex,

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic? = null,

) {

    segments.forEachIndexed { segmentIndex, segment ->

        if (segment.points.size < 2) return@forEachIndexed

        val color = TrafficPolylineStyle.previewMainRouteCoreColor()

        val width = TrafficPolylineStyle.PreviewMainRouteCoreWidthPx

        val outlineWidth = TrafficPolylineStyle.PreviewMainRouteHaloWidthPx

        val invocationId =

            renderDiagnostic?.rendererInvocationId ?: "traffic_segment_$segmentIndex"



        logPolylineRender(

            renderDiagnostic = renderDiagnostic,

            rendererInvocationId = "${invocationId}_casing",

            points = segment.points,

            zIndex = zIndexBase,

            colourCategory = RouteGeometryDiagnostic.ColourCategory.CASING,

            width = outlineWidth,

            casingRole = RouteGeometryDiagnostic.CasingRole.WHITE_OUTLINE,

            semanticRole = RouteGeometryDiagnostic.SemanticRole.TRAFFIC_SEGMENT,

            collectionPosition = segmentIndex,

        )

        Polyline(

            points = segment.points,

            color = TrafficPolylineStyle.OutlineWhite,

            width = outlineWidth,

            zIndex = zIndexBase,

            startCap = PreviewPolylineRoundCap,

            endCap = PreviewPolylineRoundCap,

            jointType = PreviewPolylineJoint,

        )

        logPolylineRender(

            renderDiagnostic = renderDiagnostic,

            rendererInvocationId = invocationId,

            points = segment.points,

            zIndex = zIndexBase + 1f,

            colourCategory = RouteGeometryDiagnostic.ColourCategory.TRAFFIC_SEGMENT,

            width = width,

            casingRole = RouteGeometryDiagnostic.CasingRole.NONE,

            semanticRole = RouteGeometryDiagnostic.SemanticRole.TRAFFIC_SEGMENT,

            collectionPosition = segmentIndex,

        )

        Polyline(

            points = segment.points,

            color = color,

            width = width,

            zIndex = zIndexBase + 1f,

            startCap = PreviewPolylineRoundCap,

            endCap = PreviewPolylineRoundCap,

            jointType = PreviewPolylineJoint,

        )

    }

}



/** Single MARSHIO-green fallback when no traffic segmentation is available. */

@Composable

@GoogleMapComposable

internal fun RenderFallbackRoutePolyline(

    points: List<LatLng>,

    density: Float,

    zIndex: Float = TrafficPolylineStyle.PreviewMainRouteHaloZIndex,

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic? = null,

) {

    if (points.size < 2) return

    val width = TrafficPolylineStyle.PreviewMainRouteCoreWidthPx

    val outlineWidth = TrafficPolylineStyle.PreviewMainRouteHaloWidthPx

    val invocationId = renderDiagnostic?.rendererInvocationId ?: "fallback_route"



    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = "${invocationId}_casing",

        points = points,

        zIndex = zIndex,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.CASING,

        width = outlineWidth,

        casingRole = RouteGeometryDiagnostic.CasingRole.WHITE_OUTLINE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.MARSHIO_SELECTED,

        collectionPosition = 0,

    )

    Polyline(

        points = points,

        color = TrafficPolylineStyle.OutlineWhite,

        width = outlineWidth,

        zIndex = zIndex,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = invocationId,

        points = points,

        zIndex = zIndex + 1f,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.MARSHIO_SELECTED,

        width = width,

        casingRole = RouteGeometryDiagnostic.CasingRole.NONE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.MARSHIO_SELECTED,

        collectionPosition = 0,

    )

    Polyline(

        points = points,

        color = TrafficPolylineStyle.previewMainRouteCoreColor(),

        width = width,

        zIndex = zIndex + 1f,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

}



@Composable

@GoogleMapComposable

internal fun RenderPreviewAlternativeRoutePolyline(

    points: List<LatLng>,

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic? = null,

) {

    if (points.size < 2) return

    val haloWidth = TrafficPolylineStyle.PreviewAlternativeHaloWidthPx

    val coreWidth = TrafficPolylineStyle.PreviewAlternativeCoreWidthPx

    val haloZIndex = TrafficPolylineStyle.PreviewAlternativeHaloZIndex

    val coreZIndex = TrafficPolylineStyle.PreviewAlternativeCoreZIndex

    val coreColor = TrafficPolylineStyle.previewAlternativeCoreColor()

    val pattern = TrafficPolylineStyle.previewAlternativePattern()

    val invocationId = renderDiagnostic?.rendererInvocationId ?: "preview_alternative"



    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = "${invocationId}_casing",

        points = points,

        zIndex = haloZIndex,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.CASING,

        width = haloWidth,

        casingRole = RouteGeometryDiagnostic.CasingRole.WHITE_OUTLINE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.GOOGLE_ALTERNATIVE,

        collectionPosition = renderDiagnostic?.collectionPosition,

    )

    Polyline(

        points = points,

        color = TrafficPolylineStyle.OutlineWhite,

        width = haloWidth,

        zIndex = haloZIndex,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = invocationId,

        points = points,

        zIndex = coreZIndex,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.GOOGLE_ALTERNATIVE,

        width = coreWidth,

        casingRole = RouteGeometryDiagnostic.CasingRole.NONE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.GOOGLE_ALTERNATIVE,

        collectionPosition = renderDiagnostic?.collectionPosition,

    )

    Polyline(

        points = points,

        color = coreColor,

        width = coreWidth,

        zIndex = coreZIndex,

        pattern = pattern,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

}



@Composable

@GoogleMapComposable

internal fun RenderPreviewSecondaryPolyline(

    points: List<LatLng>,

    color: Color,

    width: Float,

    zIndex: Float,

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic? = null,

) {

    if (points.size < 2) return

    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = renderDiagnostic?.rendererInvocationId ?: "preview_secondary",

        points = points,

        zIndex = zIndex,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.GOOGLE_ALTERNATIVE,

        width = width,

        casingRole = RouteGeometryDiagnostic.CasingRole.NONE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.GOOGLE_ALTERNATIVE,

        collectionPosition = renderDiagnostic?.collectionPosition,

    )

    Polyline(

        points = points,

        color = color,

        width = width,

        zIndex = zIndex,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

}



@Composable

@GoogleMapComposable

internal fun RenderPreviewMarshioSolidPolyline(

    points: List<LatLng>,

    density: Float,

    width: Float,

    zIndex: Float,

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic? = null,

) {

    if (points.size < 2) return

    val outlineWidth = TrafficPolylineStyle.PreviewMainRouteHaloWidthPx
    val coreWidth = TrafficPolylineStyle.PreviewMainRouteCoreWidthPx

    val invocationId = renderDiagnostic?.rendererInvocationId ?: "preview_marshio_solid"



    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = "${invocationId}_casing",

        points = points,

        zIndex = zIndex,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.CASING,

        width = outlineWidth,

        casingRole = RouteGeometryDiagnostic.CasingRole.WHITE_OUTLINE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.MARSHIO_SELECTED,

        collectionPosition = renderDiagnostic?.collectionPosition,

    )

    Polyline(

        points = points,

        color = TrafficPolylineStyle.OutlineWhite,

        width = outlineWidth,

        zIndex = zIndex,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

    logPolylineRender(

        renderDiagnostic = renderDiagnostic,

        rendererInvocationId = invocationId,

        points = points,

        zIndex = zIndex + 1f,

        colourCategory = RouteGeometryDiagnostic.ColourCategory.MARSHIO_SELECTED,

        width = coreWidth,

        casingRole = RouteGeometryDiagnostic.CasingRole.NONE,

        semanticRole = RouteGeometryDiagnostic.SemanticRole.MARSHIO_SELECTED,

        collectionPosition = renderDiagnostic?.collectionPosition,

    )

    Polyline(

        points = points,

        color = TrafficPolylineStyle.previewMainRouteCoreColor(),

        width = coreWidth,

        zIndex = zIndex + 1f,

        startCap = PreviewPolylineRoundCap,

        endCap = PreviewPolylineRoundCap,

        jointType = PreviewPolylineJoint,

    )

}



internal data class RouteGeometryPolylineRenderDiagnostic(

    val surface: RouteGeometryDiagnostic.DiagnosticSurface,

    val rendererInvocationId: String,

    val semanticRole: RouteGeometryDiagnostic.SemanticRole,

    val originalRouteIndex: Int?,

    val collectionPosition: Int? = null,

)



private fun logPolylineRender(

    renderDiagnostic: RouteGeometryPolylineRenderDiagnostic?,

    rendererInvocationId: String,

    points: List<LatLng>,

    zIndex: Float,

    colourCategory: RouteGeometryDiagnostic.ColourCategory,

    width: Float,

    casingRole: RouteGeometryDiagnostic.CasingRole,

    semanticRole: RouteGeometryDiagnostic.SemanticRole,

    collectionPosition: Int?,

) {

    val diagnostic = renderDiagnostic ?: return

    RouteGeometryDiagnostic.logPolylineRenderInput(

        surface = diagnostic.surface,

        rendererInvocationId = rendererInvocationId,

        semanticRole = semanticRole,

        originalRouteIndex = diagnostic.originalRouteIndex,

        collectionPosition = collectionPosition,

        points = points,

        zIndex = zIndex,

        colourCategory = colourCategory,

        width = width,

        casingRole = casingRole,

    )

}


