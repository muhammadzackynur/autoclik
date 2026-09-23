package com.example.auto

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        showOverlay()
    }

    private fun showOverlay() {
        val badge = TextView(this).apply {
            text = if (isEnabled()) "ON" else "OFF"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(28, 20, 28, 20)
            setBackgroundColor(if (isEnabled()) COLOR_ON else COLOR_OFF)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var downTime = 0L

        badge.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging && System.currentTimeMillis() - downTime < 300) {
                        toggleEnabled(badge)
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = badge
        windowManager.addView(badge, params)
    }

    private fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    private fun toggleEnabled(badge: TextView) {
        val newValue = !isEnabled()
        prefs.edit().putBoolean(KEY_ENABLED, newValue).apply()
        badge.text = if (newValue) "ON" else "OFF"
        badge.setBackgroundColor(if (newValue) COLOR_ON else COLOR_OFF)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
    }

    companion object {
        const val PREFS_NAME = "auto_skip_prefs"
        const val KEY_ENABLED = "ad_skip_enabled"
        const val KEY_AGGRESSIVE = "aggressive_mode"
        val COLOR_ON = Color.parseColor("#4CAF50")
        val COLOR_OFF = Color.parseColor("#F44336")
    }
}