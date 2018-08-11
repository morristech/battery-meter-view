package eo.view.batterymeter

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import eo.view.batterymeter.shape.AlertIndicator
import eo.view.batterymeter.shape.BatteryShape
import eo.view.batterymeter.shape.ChargingIndicator
import eo.view.batterymeter.util.getColorAttr

class BatteryMeterDrawable(context: Context) : Drawable() {

    companion object {
        const val MINIMUM_CHARGE_LEVEL = 0
        const val MAXIMUM_CHARGE_LEVEL = 100

        const val DEFAULT_BATTERY_COLOR_ALPHA = (0xFF * 0.3f).toInt()
        const val DEFAULT_CRITICAL_CHARGE_LEVEL = 10
    }

    private val width = context.resources.getDimensionPixelSize(R.dimen.battery_meter_width)
    private val height = context.resources.getDimensionPixelSize(R.dimen.battery_meter_height)
    private val aspectRatio = width.toFloat() / height

    private val batteryShapeBounds = Rect()

    private val batteryShape = BatteryShape(context)
    private val chargingIndicator = ChargingIndicator(context)
    private val alertIndicator = AlertIndicator(context)

    private val batteryPath = Path()
    private val indicatorPath = Path()
    private val chargeLevelPath = Path()
    private val chargeLevelClipRect = RectF()
    private val chargeLevelClipPath = Path()

    private val padding = Rect()

    private val batteryPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(
            context.getColorAttr(android.R.attr.colorForeground),
            DEFAULT_BATTERY_COLOR_ALPHA
        )
    }

    private val chargeLevelPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = context.getColorAttr(android.R.attr.colorForeground)
    }

    private val indicatorPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.TRANSPARENT
    }

    var chargeLevel: Int? = null
        set(value) {
            val newChargeLevel = value?.coerceIn(MINIMUM_CHARGE_LEVEL, MAXIMUM_CHARGE_LEVEL)
            if (newChargeLevel != field) {
                field = newChargeLevel
                updateChargeLevelPath()
                invalidateSelf()
            }
        }

    var isCharging: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                updateBatteryPath()
                invalidateSelf()
            }
        }

    var criticalChargeLevel: Int? = DEFAULT_CRITICAL_CHARGE_LEVEL
        set(value) {
            if (value != field) {
                field = value
                updateBatteryPath()
                invalidateSelf()
            }
        }

    var batteryColor: Int
        get() = batteryPaint.color
        set(value) {
            batteryPaint.color = value
            invalidateSelf()
        }

    var chargeLevelColor: Int
        get() = chargeLevelPaint.color
        set(value) {
            chargeLevelPaint.color = value
            invalidateSelf()
        }

    var indicatorColor: Int
        get() = indicatorPaint.color
        set(value) {
            indicatorPaint.color = value
            invalidateSelf()
        }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    override fun getPadding(padding: Rect): Boolean {
        if (padding.left == 0 && padding.top == 0 && padding.right == 0 && padding.bottom == 0) {
            return super.getPadding(padding)
        }

        padding.set(this.padding)
        return true
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        padding.set(left, top, right, bottom)
        updateBatteryShapeBounds()
    }

    override fun onBoundsChange(bounds: Rect) {
        updateBatteryShapeBounds()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(batteryPath, batteryPaint)
        canvas.drawPath(chargeLevelPath, chargeLevelPaint)

        if (indicatorColor != Color.TRANSPARENT) {
            canvas.drawPath(indicatorPath, indicatorPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter) {
        batteryPaint.colorFilter = colorFilter
        chargeLevelPaint.colorFilter = colorFilter
    }

    private fun updateBatteryShapeBounds() {
        if (bounds.isEmpty) return

        val availableWidth = bounds.width() - padding.left - padding.right
        val availableHeight = bounds.height() - padding.top - padding.bottom
        val availableAspectRatio = availableWidth.toFloat() / availableHeight

        if (availableAspectRatio > aspectRatio) {
            batteryShapeBounds.set(0, 0, (availableHeight * aspectRatio).toInt(), availableHeight)
        } else {
            batteryShapeBounds.set(0, 0, availableWidth, (availableWidth / aspectRatio).toInt())
        }

        batteryShapeBounds.offset(
            (availableWidth - batteryShapeBounds.width()) / 2,
            (availableHeight - batteryShapeBounds.height()) / 2
        )

        updateBatteryPath()
    }

    private fun updateBatteryPath() {
        val currentLevel = chargeLevel
        val currentCriticalLevel = criticalChargeLevel

        batteryShape.bounds = batteryShapeBounds
        batteryPath.set(batteryShape.path)

        if (currentLevel == null) {
            // TODO: show unknown indicator
        } else if (isCharging) {
            chargingIndicator.computePath(batteryShapeBounds, indicatorPath)
        } else if (currentCriticalLevel != null && currentLevel <= currentCriticalLevel) {
            alertIndicator.computePath(batteryShapeBounds, indicatorPath)
        }
        batteryPath.op(indicatorPath, Path.Op.DIFFERENCE)

        updateChargeLevelPath()
    }

    private fun updateChargeLevelPath() {
        val level = chargeLevel ?: MINIMUM_CHARGE_LEVEL
        chargeLevelClipRect.set(batteryShapeBounds)
        chargeLevelClipRect.top +=
                chargeLevelClipRect.height() * (1f - level.toFloat() / MAXIMUM_CHARGE_LEVEL)

        chargeLevelClipPath.reset()
        chargeLevelClipPath.addRect(chargeLevelClipRect, Path.Direction.CW)

        chargeLevelPath.set(batteryPath)
        chargeLevelPath.op(chargeLevelClipPath, Path.Op.INTERSECT)
    }

}