package com.clearroad.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Map pins for Route Details preview — presentation only. */
internal object MapPreviewMarkerIcons {

    private const val START_FILL = 0xFF008F73.toInt()
    private const val END_FILL = 0xFFD84315.toInt()
    private const val SPLIT_FILL = 0xFFFFA000.toInt()

    private const val PREVIEW_START_FILL = 0xFF3D5AFE.toInt()
    private const val PREVIEW_END_FILL = 0xFFEA4335.toInt()

    /** Guidance + legacy callers — unchanged. */
    fun start(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = START_FILL, diameterDp = 14f, borderDp = 2.5f)

    /** Guidance + legacy callers — unchanged. */
    fun end(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = END_FILL, diameterDp = 14f, borderDp = 2.5f)

    fun previewStart(context: Context): BitmapDescriptor =
        circlePin(
            context,
            fillColor = PREVIEW_START_FILL,
            diameterDp = 22f,
            borderDp = 3f,
        )

    fun previewEnd(context: Context): BitmapDescriptor =
        pinMarker(
            context,
            fillColor = PREVIEW_END_FILL,
            heightDp = 32f,
            strokeWidthDp = 3f,
        )

    fun split(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = SPLIT_FILL, diameterDp = 12f, borderDp = 2.5f)

    private fun circlePin(
        context: Context,
        @ColorInt fillColor: Int,
        diameterDp: Float,
        borderDp: Float = 3f,
    ): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val size = ((diameterDp + 2f * borderDp) * density).toInt().coerceAtLeast(1)
        val ring = borderDp * density
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val outerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }
        val innerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }
        canvas.drawCircle(center, center, center - 1f, outerPaint)
        canvas.drawCircle(center, center, center - ring, innerPaint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun pinMarker(
        context: Context,
        @ColorInt fillColor: Int,
        heightDp: Float,
        strokeWidthDp: Float,
    ): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val height = (heightDp * density).toInt().coerceAtLeast(1)
        val width = (height * 0.72f).toInt().coerceAtLeast(1)
        val stroke = strokeWidthDp * density
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = width / 2f
        val headRadius = width * 0.34f
        val headCenterY = headRadius + stroke
        val tipY = height - stroke

        val fillPath = Path().apply {
            addCircle(centerX, headCenterY, headRadius, Path.Direction.CW)
            moveTo(centerX - headRadius * 0.55f, headCenterY + headRadius * 0.35f)
            lineTo(centerX, tipY)
            lineTo(centerX + headRadius * 0.55f, headCenterY + headRadius * 0.35f)
            close()
        }

        val strokePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.STROKE
                this.strokeWidth = stroke
                strokeJoin = Paint.Join.ROUND
            }
        val fillPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style = Paint.Style.FILL
            }

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(fillPath, strokePaint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
