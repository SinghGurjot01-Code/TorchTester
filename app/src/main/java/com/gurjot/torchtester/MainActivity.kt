package com.gurjot.torchtester

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var maxStrength = 1
    private var strength = 1
    private var torchOn = false

    private lateinit var status: TextView
    private lateinit var levelText: TextView
    private lateinit var slider: VerticalIntensitySlider
    private lateinit var toggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 13, 16)
        window.navigationBarColor = Color.rgb(12, 13, 16)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        buildUi()
        detectTorch()
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
    }

    // Keeps UI in sync when the torch is switched off/on by something else
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId || enabled == torchOn) return
            torchOn = enabled
            toggle.text = if (enabled) "TURN TORCH OFF" else "TURN TORCH ON"
            if (!enabled) status.text = "Torch off · hardware exposes $maxStrength level(s)"
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(36), dp(24), dp(24))
            setBackgroundColor(Color.rgb(12, 13, 16))
        }

        val title = TextView(this).apply {
            text = "TORCH TESTER"
            setTextColor(Color.WHITE)
            textSize = 23f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.08f
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Hardware brightness diagnostic"
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, dp(8), 0, dp(28))
        }
        root.addView(subtitle)

        status = TextView(this).apply {
            text = "Checking camera hardware…"
            setTextColor(Color.rgb(190, 195, 205))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        root.addView(status)

        levelText = TextView(this).apply {
            text = "LEVEL 1"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(24), 0, dp(12))
        }
        root.addView(levelText)

        slider = VerticalIntensitySlider(this).apply {
            maxValue = maxStrength
            value = strength
            onValueChanged = { selected ->
                strength = selected
                updateLevelLabel()
                if (torchOn) applyStrength()
            }
        }
        root.addView(slider, LinearLayout.LayoutParams(dp(90), 0, 1f))

        val hint = TextView(this).apply {
            text = "SLIDE UP  ↑  BRIGHTER     DIMMER  ↓"
            setTextColor(Color.rgb(150, 155, 165))
            textSize = 11f
            setPadding(0, dp(12), 0, dp(20))
        }
        root.addView(hint)

        toggle = Button(this).apply {
            text = "TURN TORCH ON"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(235, 125, 45))
            setOnClickListener { toggleTorch() }
        }
        root.addView(toggle, LinearLayout.LayoutParams(-1, dp(54)))
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(dp(24), dp(36) + bars.top, dp(24), dp(24) + bars.bottom)
            }
            insets
        }
        setContentView(root)
    }

    private fun detectTorch() {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            val id = cameraId
            if (id == null) {
                status.text = "No compatible camera flash found."
                toggle.isEnabled = false
                slider.isEnabled = false
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(id)
            maxStrength = if (Build.VERSION.SDK_INT >= 33) {
                characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } else 1

            strength = maxStrength.coerceAtLeast(1)
            slider.maxValue = maxStrength
            slider.value = strength
            updateLevelLabel()

            status.text = if (maxStrength > 1) {
                "Adjustable torch detected · $maxStrength levels"
            } else {
                "Flash detected · fixed brightness exposed by Android"
            }
        } catch (e: Exception) {
            status.text = "Hardware check failed: ${e.javaClass.simpleName}"
        }
    }

    private fun updateLevelLabel() {
        levelText.text = if (maxStrength > 1) "LEVEL $strength / $maxStrength" else "FIXED BRIGHTNESS"
        slider.invalidate()
    }

    private fun toggleTorch() {
        val id = cameraId ?: run {
            status.text = "No flash available."
            return
        }
        try {
            if (torchOn) {
                cameraManager.setTorchMode(id, false)
                torchOn = false
                toggle.text = "TURN TORCH ON"
                status.text = "Torch off · hardware exposes $maxStrength level(s)"
            } else {
                if (Build.VERSION.SDK_INT >= 33 && maxStrength > 1) {
                    cameraManager.turnOnTorchWithStrengthLevel(id, strength)
                } else {
                    cameraManager.setTorchMode(id, true)
                }
                torchOn = true
                toggle.text = "TURN TORCH OFF"
                status.text = if (maxStrength > 1) "Torch on · slider controls torch strength"
                    else "Torch on · this device exposes fixed brightness"
            }
        } catch (e: Exception) {
            torchOn = false
            toggle.text = "TURN TORCH ON"
            status.text = "Torch command failed: ${e.javaClass.simpleName}"
        }
    }

    private fun applyStrength() {
        val id = cameraId ?: return
        if (Build.VERSION.SDK_INT < 33 || maxStrength <= 1) return
        try {
            cameraManager.turnOnTorchWithStrengthLevel(id, strength)
        } catch (e: Exception) {
            status.text = "Strength change rejected: ${e.javaClass.simpleName}"
        }
    }

    override fun onPause() {
        super.onPause()
        if (torchOn) {
            try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (_: Exception) {}
            torchOn = false
            if (::toggle.isInitialized) toggle.text = "TURN TORCH ON"
        }
    }

    override fun onDestroy() {
        cameraManager.unregisterTorchCallback(torchCallback)
        super.onDestroy()
    }
}

private class VerticalIntensitySlider(context: android.content.Context) : View(context) {
    var maxValue: Int = 1
        set(value) { field = value.coerceAtLeast(1); invalidate() }
    var value: Int = 1
        set(value) { field = value.coerceIn(1, maxValue); invalidate() }
    var onValueChanged: ((Int) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(48, 51, 59) }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 137, 57) }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bounds = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val top = 18f
        val bottom = height - 18f
        val trackWidth = 12f
        bounds.set(cx - trackWidth / 2, top, cx + trackWidth / 2, bottom)
        canvas.drawRoundRect(bounds, 8f, 8f, trackPaint)

        val fraction = if (maxValue <= 1) 1f else (value - 1).toFloat() / (maxValue - 1)
        val knobY = bottom - fraction * (bottom - top)
        bounds.set(cx - trackWidth / 2, knobY, cx + trackWidth / 2, bottom)
        canvas.drawRoundRect(bounds, 8f, 8f, fillPaint)
        canvas.drawCircle(cx, knobY, 15f, knobPaint)
        canvas.drawCircle(cx, knobY, 8f, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val top = 18f
                val bottom = (height - 18f).coerceAtLeast(top + 1f)
                val fraction = (1f - ((event.y - top) / (bottom - top))).coerceIn(0f, 1f)
                val selected = if (maxValue <= 1) 1 else
                    Math.round(1 + fraction * (maxValue - 1)).coerceIn(1, maxValue)
                if (selected != value) {
                    value = selected
                    onValueChanged?.invoke(value)
                }
                return true
            }
        }
        return true
    }
}
