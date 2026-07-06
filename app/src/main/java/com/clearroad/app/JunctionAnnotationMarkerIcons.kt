package com.clearroad.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

internal object JunctionAnnotationMarkerIcons {
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()

    fun card(
        context: Context,
        annotation: RouteJunctionAnnotation,
    ): BitmapDescriptor {
        val cacheKey = annotation.type.name
        iconCache[cacheKey]?.let { return it }

        val density = context.resources.displayMetrics.density
        val horizontalPaddingPx = 4f * density
        val verticalPaddingPx = 3f * density
        val cornerRadiusPx = 8f * density
        val shadowOffsetPx = 2f * density
        val textSizePx = 10f * density
        val iconSizePx = 12f * density
        val iconGapPx = 3f * density

        val label = JunctionAnnotationClassifier.displayLabel(annotation.type)
        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF333333.toInt()
                textSize = textSizePx
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
        val labelWidth = textPaint.measureText(label)
        val contentWidth = iconSizePx + iconGapPx + labelWidth
        val cardWidth = contentWidth + horizontalPaddingPx * 2f
        val cardHeight = iconSizePx + verticalPaddingPx * 2f

        val bitmapWidth = (cardWidth + shadowOffsetPx).toInt().coerceAtLeast(1)
        val bitmapHeight = (cardHeight + shadowOffsetPx).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val shadowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x40000000
                style = Paint.Style.FILL
            }
        val cardPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }

        val cardRect = RectF(0f, 0f, cardWidth, cardHeight)
        val shadowRect =
            RectF(
                0f,
                shadowOffsetPx,
                cardWidth,
                cardHeight + shadowOffsetPx,
            )
        canvas.drawRoundRect(shadowRect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
        canvas.drawRoundRect(cardRect, cornerRadiusPx, cornerRadiusPx, cardPaint)

        val iconCenterX = horizontalPaddingPx + iconSizePx / 2f
        val iconCenterY = cardHeight / 2f
        drawTypeIcon(
            canvas = canvas,
            type = annotation.type,
            centerX = iconCenterX,
            centerY = iconCenterY,
            sizePx = iconSizePx,
            density = density,
        )

        val textX = horizontalPaddingPx + iconSizePx + iconGapPx
        val baselineY = cardHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, textX, baselineY, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap).also { iconCache[cacheKey] = it }
    }

    private fun drawTypeIcon(
        canvas: Canvas,
        type: JunctionType,
        centerX: Float,
        centerY: Float,
        sizePx: Float,
        density: Float,
    ) {
        when (type) {
            JunctionType.ROUNDABOUT -> {
                val strokePx = 1.5f * density
                val ringPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = JunctionAnnotationClassifier.iconColor(type)
                        style = Paint.Style.STROKE
                        strokeWidth = strokePx
                    }
                canvas.drawCircle(centerX, centerY, sizePx / 2f - strokePx, ringPaint)
            }
            JunctionType.TOLL -> {
                val emojiPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = sizePx
                    }
                val glyph = JunctionAnnotationClassifier.iconGlyph(type)
                val glyphWidth = emojiPaint.measureText(glyph)
                canvas.drawText(
                    glyph,
                    centerX - glyphWidth / 2f,
                    centerY - (emojiPaint.descent() + emojiPaint.ascent()) / 2f,
                    emojiPaint,
                )
            }
            else -> {
                val iconPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = JunctionAnnotationClassifier.iconColor(type)
                        textSize = sizePx
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    }
                val glyph =
                    if (JunctionAnnotationClassifier.usesColoredVectorIcon(type)) {
                        JunctionAnnotationClassifier.vectorIconGlyph(type)
                    } else {
                        JunctionAnnotationClassifier.iconGlyph(type)
                    }
                val glyphWidth = iconPaint.measureText(glyph)
                canvas.drawText(
                    glyph,
                    centerX - glyphWidth / 2f,
                    centerY - (iconPaint.descent() + iconPaint.ascent()) / 2f,
                    iconPaint,
                )
            }
        }
    }
}
