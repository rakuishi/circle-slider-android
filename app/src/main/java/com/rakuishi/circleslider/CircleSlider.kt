package com.rakuishi.circleslider

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class CircleSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val circleSize: Float = dp2px(16f)
    private val thumbSize: Float = dp2px(24f)
    private val thumbBlurRadius: Float = dp2px(4f)
    private val thumbBlurContainSize: Float = thumbSize + thumbBlurRadius * 2

    // TODO: 画面サイズを考慮したサイズに変更する
    // 250 を基準に thumb の飛び出た部分を考慮したサイズにする
    private val bgSize: Float = dp2px(250f + (thumbBlurContainSize - circleSize))
    private val center: Float = bgSize / 2
    private val circlePaint: Paint = Paint()
    private val circleActivePaint: Paint = Paint()
    private val thumbPaint: Paint = Paint()
    private val thumbShadowPaint: Paint = Paint()

    // 右(1, 0) を基準に時計回りに進む
    // デフォルトでは、可動域は 120° から 300° 進んだ 420° の範囲。下方向に 60° 穴が空いたランドルト環を想定
    private var holeAngle: Float = 60f
    private var startAngle: Float = 90f + holeAngle / 2f // 下(0, -1) を基準に thumb を動かす
    private var currentThumbAngle: Float = startAngle
    private var division: Int = 20

    private var rectF = RectF()

    init {
        circlePaint.run {
            isAntiAlias = true
            color = Color.parseColor("#99DDBF")
            strokeWidth = circleSize
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        circleActivePaint.run {
            isAntiAlias = true
            color = Color.parseColor("#43BF83")
            strokeWidth = circleSize
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        thumbPaint.run {
            isAntiAlias = true
            color = Color.parseColor("#FFFFFF")
        }

        thumbShadowPaint.run {
            isAntiAlias = true
            color = Color.parseColor("#CCCCCC")
            setMaskFilter(
                BlurMaskFilter(
                    thumbBlurRadius,
                    BlurMaskFilter.Blur.NORMAL
                )
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(bgSize.toInt(), bgSize.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
        drawThumb(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        val halfThumbSize = thumbBlurContainSize / 2f
        rectF.set(
            halfThumbSize,
            halfThumbSize,
            bgSize - halfThumbSize,
            bgSize - halfThumbSize
        )

        // 右(1, 0) から 120° 進んだ位置から 300° 分描く
        canvas.drawArc(rectF, startAngle, 360f - holeAngle, false, circlePaint)
        canvas.drawArc(rectF, startAngle, currentThumbAngle - startAngle, false, circleActivePaint)
    }

    private fun drawThumb(canvas: Canvas) {
        val r = bgSize / 2f - thumbBlurContainSize / 2f
        val x = bgSize / 2f + cos(currentThumbAngle * Math.PI / 180f) * r
        val y = bgSize / 2f + sin(currentThumbAngle * Math.PI / 180f) * r

        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        canvas.drawCircle(0f, 0f, thumbSize / 2f, thumbShadowPaint)
        canvas.drawCircle(0f, 0f, thumbSize / 2f, thumbPaint)
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                updateCurrentThumbAngleIfNeeded(getAngle(event.y, event.x))
            }
        }
        return true
    }

    private fun updateCurrentThumbAngleIfNeeded(angle: Float) {
        // 計算しやすいように 下(0, -1) = 90° ~ 一周回って最大 450° に角度を調整する
        val correctedAngle = if (angle in 0.0..90.0) angle + 360 else angle
        val divisionAngle = (360.0 - holeAngle) / division

        // 0 の場合は startAngle の左右に divisionAngle / 2.0 分の領域となる
        val baseAngle = startAngle - divisionAngle / 2.0

        for (i in 0..division) {
            if (baseAngle + (i * divisionAngle) <= correctedAngle
                && correctedAngle < baseAngle + ((i + 1) * divisionAngle)
            ) {
                currentThumbAngle = (startAngle + i * divisionAngle).toFloat()
                postInvalidate()
                break
            }
        }
    }

    // 右(1, 0) を基準に時計回りに進むように調整する
    private fun getAngle(x: Float, y: Float): Float {
        return ((atan2(-(y - center), x - center) * 180f / Math.PI + 450f) % 360f).toFloat()
    }

    private fun dp2px(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}