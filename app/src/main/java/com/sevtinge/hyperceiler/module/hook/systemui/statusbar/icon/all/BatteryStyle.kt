/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import de.robv.android.xposed.*

object BatteryStyle : BaseHook() {
    private val fontSize by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_size", 15) * 0.5f
    }
    private val fontSizeMark by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_font_mark_size", 15) * 0.5f
    }
    private val verticalOffset by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset", 8)
    }
    private val verticalOffsetMark by lazy {
        mPrefsMap.getInt("system_ui_status_bar_battery_style_vertical_offset_mark", 27)
    }
    private val isChangeLocation by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location")
    }
    private val isHideText by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")
    }
    private val isEnableCustom by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom")
    }
    private val isEnableBold by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_style_bold")
    }
    private val isEnableBatteryMark by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")
    }
    private val isPixelStyle by lazy {
        val key = "prefs_key_system_ui_status_bar_battery_style_pixel"
        !mPrefsMap.containsKey(key) || mPrefsMap.getBoolean("system_ui_status_bar_battery_style_pixel")
    }

    private val mBatteryMeterViewClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterView")
    }
    private val mBatteryMeterIconViewClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiBatteryMeterIconView")
    }

    override fun init() {
        if (isMoreAndroidVersion(35)) {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateAll\$1")
        } else {
            mBatteryMeterViewClass.methodFinder()
                .filterByName("updateAll")
        }.single().createHook {
            after { param ->
                hookStatusBattery(param)
            }
        }

        if (isPixelStyle) {
            arrayOf(
                "onFinishInflate",
                "onAttachedToWindow",
                "onBatteryLevelChanged",
                "onBatteryStyleChanged",
                "onChargeStateChanged",
                "updateChargeAndText",
                "onDarkChangedInternal"
            ).forEach { methodName ->
                XposedBridge.hookAllMethods(
                    mBatteryMeterViewClass,
                    methodName,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            applyPixelStyle(param.thisObject)
                        }
                    }
                )
            }

            arrayOf(
                "onFinishInflate",
                "onBatteryLevelChanged",
                "onBatteryStyleChanged",
                "onChargeStateChanged",
                "onDarkChanged",
                "onDarkChangeInternal",
                "updateResources"
            ).forEach { methodName ->
                XposedBridge.hookAllMethods(
                    mBatteryMeterIconViewClass,
                    methodName,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            applyPixelIcon(param.thisObject as ImageView, param.thisObject)
                        }
                    }
                )
            }
        }
    }

    private fun changeLocation(
        batteryView: LinearLayout,
        mBatteryPercentView: TextView,
        mBatteryPercentMarkView: TextView
    ) {
        batteryView.removeView(mBatteryPercentView)
        batteryView.removeView(mBatteryPercentMarkView)
        batteryView.addView(mBatteryPercentMarkView, 0)
        batteryView.addView(mBatteryPercentView, 0)
    }

    private fun setBatterySize(view: TextView, size: Float) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }

    private fun setMargin(view1: TextView, view2: TextView) {
        // 左侧间距
        var leftMargin =
            mPrefsMap.getInt("system_ui_status_bar_battery_style_left_margin", 0)
        leftMargin = dp2px(leftMargin * 0.5f)

        // 右侧间距
        var rightMargin =
            mPrefsMap.getInt("system_ui_status_bar_battery_style_right_margin", 0)
        rightMargin = dp2px(rightMargin * 0.5f)

        // 上下偏移量
        var topMargin = 0
        if (verticalOffset != 12) {
            topMargin = dp2px((verticalOffset - 12) * 0.5f)
        }
        view1.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

        var digitRightMargin = 0
        var markRightMargin = 0
        if (isEnableBatteryMark) {
            digitRightMargin = rightMargin
        } else {
            markRightMargin = rightMargin
        }
        if (leftMargin > 0 || topMargin != 8 || digitRightMargin > 0) {
            view1.setPaddingRelative(
                leftMargin, topMargin, digitRightMargin, 0
            )
        }

        if (verticalOffsetMark < 27) {
            val marginTop =
                dp2px((verticalOffsetMark - 8) * 0.5f)
            topMargin = marginTop
        }
        if (verticalOffsetMark < 27 || markRightMargin > 0) {
            view2.setPaddingRelative(0, topMargin, markRightMargin, 0)
        }
    }

    private fun hookStatusBattery(param: XC_MethodHook.MethodHookParam) {
        val batteryView = param.thisObject as LinearLayout
        val mBatteryPercentView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView") as TextView
        val mBatteryPercentMarkView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView") as TextView
        val mBatteryTextDigitView =
            XposedHelpers.getObjectField(param.thisObject, "mBatteryTextDigitView") as TextView

        if (isPixelStyle) {
            mBatteryTextDigitView.visibility = android.view.View.GONE
            applyPixelStyle(param.thisObject)
        }

        // 交换电池图标与电量位置（电量外显下才能正常交换）
        if (isChangeLocation) {
            changeLocation(batteryView, mBatteryPercentView, mBatteryPercentMarkView)
        }

        // 以下功能需要启用修改
        if (!isHideText && isEnableCustom) {
            if (fontSize > 7.5) {
                setBatterySize(mBatteryTextDigitView, fontSize)
                setBatterySize(mBatteryPercentView, fontSize)
            }
            if (fontSizeMark > 7.5) {
                setBatterySize(mBatteryPercentMarkView, fontSizeMark)
            }

            if (isEnableBold) {
                mBatteryTextDigitView.typeface = Typeface.DEFAULT_BOLD
                mBatteryPercentView.typeface = Typeface.DEFAULT_BOLD
            }

            // 设置边距
            setMargin(mBatteryPercentView, mBatteryPercentMarkView)
        }
    }

    private fun applyPixelStyle(batteryMeterView: Any) {
        val iconView = runCatching {
            XposedHelpers.getObjectField(batteryMeterView, "mBatteryIconView") as ImageView
        }.getOrNull() ?: return

        applyPixelIcon(iconView, iconView)

        runCatching {
            (XposedHelpers.getObjectField(batteryMeterView, "mBatteryTextDigitView") as TextView).visibility =
                android.view.View.GONE
        }

        runCatching {
            (XposedHelpers.getObjectField(batteryMeterView, "mBatteryDigitalView") as FrameLayout).visibility =
                android.view.View.VISIBLE
        }

        sequenceOf("mBatteryChargingInView", "mBatteryChargingView").forEach { field ->
            runCatching {
                (XposedHelpers.getObjectField(batteryMeterView, field) as ImageView).visibility =
                    android.view.View.GONE
            }
        }
    }

    private fun applyPixelIcon(iconView: ImageView, stateOwner: Any) {
        val previousLevel = iconView.drawable?.level?.takeIf { it in 1..100 }
        val level = sequenceOf("mLevel", "mBatteryLevel")
            .mapNotNull { field ->
                runCatching { XposedHelpers.getIntField(stateOwner, field) }.getOrNull()
            }
            .firstOrNull { it in 0..100 }
            ?: previousLevel
            ?: 100
        val charging = sequenceOf("mCharging", "mIsCharging", "mPlugged")
            .mapNotNull { field ->
                runCatching { XposedHelpers.getBooleanField(stateOwner, field) }.getOrNull()
            }
            .firstOrNull()
            ?: false

        val drawable = (iconView.drawable as? PixelBatteryDrawable)
            ?: PixelBatteryDrawable(iconView.resources.displayMetrics.density)
        drawable.update(level, charging, resolvePixelColor(stateOwner))

        iconView.setImageDrawable(drawable)
        iconView.visibility = android.view.View.VISIBLE
        iconView.scaleType = ImageView.ScaleType.CENTER
        iconView.setPadding(0, 0, 0, 0)
        iconView.layoutParams?.let { layoutParams ->
            layoutParams.width = dp2px(21f)
            layoutParams.height = dp2px(12f)
            iconView.layoutParams = layoutParams
            iconView.requestLayout()
        }
    }

    private fun resolvePixelColor(stateOwner: Any): Int {
        val useTint = runCatching {
            XposedHelpers.getBooleanField(stateOwner, "mUseTint")
        }.getOrDefault(false)
        val colorField = if (useTint) {
            "mTintColor"
        } else if (runCatching {
                XposedHelpers.getIntField(stateOwner, "mDark")
            }.getOrDefault(1) == 2
        ) {
            "mDarkColor"
        } else {
            "mLightColor"
        }
        return runCatching {
            XposedHelpers.getIntField(stateOwner, colorField)
        }.getOrNull()?.takeIf { Color.alpha(it) != 0 } ?: Color.WHITE
    }

    private class PixelBatteryDrawable(private val density: Float) : Drawable() {
        private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.25f * density
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        private var batteryLevel = 100
        private var isCharging = false

        fun update(level: Int, charging: Boolean, color: Int) {
            batteryLevel = level.coerceIn(0, 100)
            isCharging = charging
            outlinePaint.color = color
            fillPaint.color = color
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) {
            val availableWidth = bounds.width().toFloat()
            val availableHeight = bounds.height().toFloat()
            if (availableWidth <= 0f || availableHeight <= 0f) return

            val iconWidth = minOf(availableWidth, 21f * density)
            val iconHeight = minOf(availableHeight, 11f * density)
            val left = bounds.left + (availableWidth - iconWidth) / 2f
            val top = bounds.top + (availableHeight - iconHeight) / 2f
            val terminalWidth = 1.5f * density
            val strokeInset = outlinePaint.strokeWidth / 2f

            val layer = canvas.saveLayer(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), null)
            canvas.drawRoundRect(
                left + iconWidth - terminalWidth - 0.4f * density,
                top + iconHeight * 0.3f,
                left + iconWidth,
                top + iconHeight * 0.7f,
                1.2f * density,
                1.2f * density,
                fillPaint
            )

            val body = RectF(
                left + strokeInset,
                top + strokeInset,
                left + iconWidth - terminalWidth - strokeInset,
                top + iconHeight - strokeInset
            )
            canvas.drawRoundRect(body, 2f * density, 2f * density, outlinePaint)

            val inner = RectF(body).apply { inset(1.35f * density, 1.35f * density) }
            val fillRight = inner.left + inner.width() * batteryLevel / 100f
            canvas.drawRoundRect(
                inner.left,
                inner.top,
                fillRight,
                inner.bottom,
                0.8f * density,
                0.8f * density,
                fillPaint
            )

            if (isCharging) {
                val centerX = body.centerX()
                val centerY = body.centerY()
                val bolt = Path().apply {
                    moveTo(centerX + 0.4f * density, centerY - 3.2f * density)
                    lineTo(centerX - 1.8f * density, centerY + 0.2f * density)
                    lineTo(centerX - 0.2f * density, centerY + 0.2f * density)
                    lineTo(centerX - 0.6f * density, centerY + 3.2f * density)
                    lineTo(centerX + 1.8f * density, centerY - 0.7f * density)
                    lineTo(centerX + 0.25f * density, centerY - 0.7f * density)
                    close()
                }
                canvas.drawPath(bolt, clearPaint)
            }
            canvas.restoreToCount(layer)
        }

        override fun setAlpha(alpha: Int) {
            outlinePaint.alpha = alpha
            fillPaint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            outlinePaint.colorFilter = colorFilter
            fillPaint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = (21f * density).toInt()

        override fun getIntrinsicHeight(): Int = (12f * density).toInt()
    }
}
