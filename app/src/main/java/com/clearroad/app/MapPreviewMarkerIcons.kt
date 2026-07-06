package com.clearroad.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Large map pins for Route Details preview — presentation only. */
internal object MapPreviewMarkerIcons {

    private const val START_FILL = 0xFF1E88E5.toInt()
    private const val END_FILL = 0xFFE53935.toInt()
    private const val SPLIT_FILL = 0xFFFFA000.toInt()

    fun start(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = START_FILL, diameterDp = 8f, borderDp = 2f)

    fun end(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = END_FILL, diameterDp = 8f, borderDp = 2f)

    fun split(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = SPLIT_FILL, diameterDp = 32f)

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
}
