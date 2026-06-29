package com.clearroad.app.guidance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

internal object GuidanceMarkerIcons {

    private const val GUIDANCE_FILL = 0xFF2196F3.toInt()

    fun guidanceDot(context: Context): BitmapDescriptor =
        circlePin(context, fillColor = GUIDANCE_FILL, diameterDp = 28f)

    private fun circlePin(
        context: Context,
        fillColor: Int,
        diameterDp: Float,
    ): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val size = (diameterDp * density).toInt().coerceAtLeast(28)
        val ring = (3f * density).coerceAtLeast(4f)
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
