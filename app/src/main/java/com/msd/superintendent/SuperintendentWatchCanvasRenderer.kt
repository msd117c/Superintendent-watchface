package com.msd.superintendent

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.TypedValue
import android.view.SurfaceHolder
import androidx.appcompat.content.res.AppCompatResources
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime


private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

class SuperintendentWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<SuperintendentWatchCanvasRenderer.DigitalSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class DigitalSharedAssets : SharedAssets {
        override fun onDestroy() {}
    }

    private var clockCenterX = 0f
    private var clockCenterY = 0f

    private val faceStrokeRadius: Float by lazy { context.resources.getDimension(R.dimen.superintendent_radius).toPx }
    private val faceStrokeWidth: Float by lazy { faceStrokeRadius.times(0.125f) }
    private val faceFillRadius: Float by lazy { faceStrokeRadius.times(0.975f) }
    private val eyeDistanceToCenter: Float by lazy { faceFillRadius.times(0.43589744f) }
    private val eyeRadius: Float by lazy { faceStrokeRadius.times(0.3f) }

    private var superintendentCenterX = 0f
    private var superintendentCenterY = 0f

    private val leftEyeCenterX: Float by lazy { superintendentCenterX - eyeDistanceToCenter }
    private val rightEyeCenterX: Float by lazy { superintendentCenterX + eyeDistanceToCenter }
    private val eyesCenterY: Float by lazy { superintendentCenterY }
    private val leftEyeOval: RectF by lazy {
        RectF(
            leftEyeCenterX - eyeRadius,
            eyesCenterY - eyeRadius,
            leftEyeCenterX + eyeRadius,
            eyesCenterY + eyeRadius
        )
    }
    private val rightEyeOval: RectF by lazy {
        RectF(
            rightEyeCenterX - eyeRadius,
            eyesCenterY - eyeRadius,
            rightEyeCenterX + eyeRadius,
            eyesCenterY + eyeRadius
        )
    }

    private var additionalTextCenterX = 0f
    private var additionalTextCenterY = 0f

    private var ambientTextCenterX = 0f
    private var ambientTextCenterY = 0f

    private var multiLineTextCenterX = 0f
    private var multiLineTextCenterY = 0f

    private var imageCenterX = 0f
    private var imageCenterY = 0f
    private val imageWidth: Float by lazy { context.resources.getDimension(R.dimen.image_width).toPx }
    private val imageHeight: Int by lazy { imageWidth.times(1.408f).toInt() }

    private val textFont: Typeface by lazy { context.resources.getFont(R.font.evogria) }

    private val clockPaint = Paint().apply {
        isAntiAlias = true
        typeface = textFont
        textSize = context.resources.getDimensionPixelSize(R.dimen.digits_size).toFloat()
        textAlign = Paint.Align.CENTER
        color = context.getColor(R.color.white)
    }
    private val multiLineTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = textFont
        textSize = context.resources.getDimensionPixelSize(R.dimen.multi_line_text_size).toFloat()
        textAlign = Paint.Align.LEFT
        color = context.getColor(R.color.white)
    }
    private val additionalTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = textFont
        textSize = context.resources.getDimensionPixelSize(R.dimen.additional_text_size).toFloat()
        textAlign = Paint.Align.CENTER
        color = context.getColor(R.color.white)
    }
    private val ambientTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = textFont
        textSize = context.resources.getDimensionPixelSize(R.dimen.ambient_text_size).toFloat()
        textAlign = Paint.Align.CENTER
        color = context.getColor(android.R.color.holo_red_dark)
    }

    private val faceStrokePaint = Paint().apply {
        strokeWidth = faceStrokeWidth
        color = context.getColor(R.color.white)
        style = Paint.Style.STROKE
    }
    private val faceFillPaint = Paint().apply {
        strokeWidth = 0f
        style = Paint.Style.FILL
    }
    private val eyesPaint = Paint().apply {
        isAntiAlias = true
        color = context.getColor(R.color.white)
        style = Paint.Style.FILL
    }

    override suspend fun createSharedAssets(): DigitalSharedAssets {
        return DigitalSharedAssets()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
        canvas.apply {
            if (clockCenterX == 0f) {
                clockCenterX = centerX
                clockCenterY = height.div(2f)

                multiLineTextCenterX = width.div(4f)
                multiLineTextCenterY = height.div(6f)

                superintendentCenterX = centerX
                superintendentCenterY = height.times(5f).div(7f)

                additionalTextCenterX = superintendentCenterX
                additionalTextCenterY = superintendentCenterY + faceStrokeRadius + 30f

                imageCenterX = width.times(2f).div(3f)

                ambientTextCenterX = centerX
                ambientTextCenterY = height.div(5f)
            }

            drawBackground()

            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
                drawClock(zonedDateTime)
            }

            if (renderParameters.drawMode == DrawMode.INTERACTIVE &&
                renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)
            ) {
                drawOuterElements(zonedDateTime)
            }
        }
    }

    private fun Canvas.drawBackground() {
        drawColor(context.getColor(R.color.black))
    }

    private fun Canvas.drawClock(zonedDateTime: ZonedDateTime) {
        val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT

        val text = with(zonedDateTime) {
            if (drawAmbient) {
                "$hour:${formatDigits(minute)}"
            } else {
                "$hour:${formatDigits(minute)}:${formatDigits(second)}"
            }
        }

        drawText(text, clockCenterX, clockCenterY, clockPaint)

        if (drawAmbient) {
            drawFace(EyesType.IDLE)
            drawEyes(EyesType.IDLE)
            drawAdditionalText()
            drawAmbientText()
        }
    }

    private fun formatDigits(digit: Int) = String.format("%02d", digit)

    // private var lastChange: Long = 0
    // private var idx = 0
    private var eyesType: EyesType = EyesType.IDLE

    private fun Canvas.drawOuterElements(time: ZonedDateTime) {
        drawMultilineText()
        drawAdditionalText()

        // if (time.toInstant().toEpochMilli() - lastChange > 3000) {
        //     lastChange = time.toInstant().toEpochMilli()
        //     eyesType = EyesType.values()[idx]
        //     idx++
        //     if (idx >= EyesType.values().size) {
        //         idx = 0
        //     }
        // }

        drawFace(eyesType)
        drawEyes(eyesType)
    }

    private fun Canvas.drawMultilineText() {
        val text = ">NAVIGATION\n>SYSTEM\n>TIME\n>INTEL"

        multiLineTextCenterY = height.div(6f)
        for (line in text.split("\n").toTypedArray()) {
            multiLineTextPaint.color = if (line == ">TIME") {
                context.getColor(R.color.gray)
            } else {
                context.getColor(R.color.white)
            }
            if (line == ">NAVIGATION") {
                imageCenterY =
                    multiLineTextCenterY + multiLineTextPaint.descent() - multiLineTextPaint.ascent()
            }
            drawText(line, multiLineTextCenterX, multiLineTextCenterY, multiLineTextPaint)
            multiLineTextCenterY += multiLineTextPaint.descent() - multiLineTextPaint.ascent()
        }
        val svg = AppCompatResources.getDrawable(context, R.drawable.ic_unsc)?.apply {
            setTint(context.getColor(R.color.white))
            bounds = Rect(
                (imageCenterX - imageWidth.div(2)).toInt(),
                (imageCenterY - imageHeight.div(2)).toInt(),
                (imageCenterX + imageWidth.div(2)).toInt(),
                (imageCenterY + imageHeight.div(2)).toInt()
            )
        }
        svg?.draw(this)
    }

    private fun Canvas.drawAdditionalText() {
        val text = ">>PLEASE REMAIN CALM<<"
        drawText(text, additionalTextCenterX, additionalTextCenterY, additionalTextPaint)
    }

    private fun Canvas.drawAmbientText() {
        val text = ">>ACCESS DENIED<<"
        drawText(text, ambientTextCenterX, ambientTextCenterY, ambientTextPaint)
    }

    private fun Canvas.drawFace(eyesType: EyesType) {
        faceFillPaint.color = if (eyesType == EyesType.ANGRY) {
            context.getColor(android.R.color.holo_red_dark)
        } else {
            context.getColor(R.color.superintendent_normal)
        }

        drawCircle(superintendentCenterX, superintendentCenterY, faceStrokeRadius, faceStrokePaint)
        drawCircle(superintendentCenterX, superintendentCenterY, faceFillRadius, faceFillPaint)
    }

    private fun Canvas.drawEyes(eyesType: EyesType) {
        when (eyesType) {
            EyesType.IDLE -> {
                drawCircle(leftEyeCenterX, eyesCenterY, eyeRadius, eyesPaint)
                drawCircle(rightEyeCenterX, eyesCenterY, eyeRadius, eyesPaint)
            }
            EyesType.HAPPY -> {
                drawArc(leftEyeOval, 180f, 180f, true, eyesPaint)
                drawArc(rightEyeOval, 180f, 180f, true, eyesPaint)
            }
            EyesType.BORED -> {
                drawArc(leftEyeOval, 180f, -180f, true, eyesPaint)
                drawArc(rightEyeOval, 180f, -180f, true, eyesPaint)
            }
            EyesType.CONFUSED -> {
                drawCircle(leftEyeCenterX, eyesCenterY, eyeRadius, eyesPaint)
                drawArc(rightEyeOval, 10f, 180f, true, eyesPaint)
            }
            EyesType.SUSPICIOUS -> {
                drawCircle(leftEyeCenterX, eyesCenterY, eyeRadius, eyesPaint)
                drawArc(rightEyeOval, 180f, -180f, true, eyesPaint)
            }
            EyesType.SAD -> {
                drawArc(leftEyeOval, -20f, 180f, true, eyesPaint)
                drawArc(rightEyeOval, 20f, 180f, true, eyesPaint)
            }
            EyesType.ANGRY -> {
                drawArc(leftEyeOval, 20f, 180f, true, eyesPaint)
                drawArc(rightEyeOval, -20f, 180f, true, eyesPaint)
            }
        }
    }

    private val Number.toPx
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )

    enum class EyesType {
        IDLE,
        HAPPY,
        BORED,
        CONFUSED,
        SUSPICIOUS,
        SAD,
        ANGRY
    }
}