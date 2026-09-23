package com.example.auto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipService : AccessibilityService() {

    private var isProcessingClick = false
    private lateinit var prefs: SharedPreferences
    private var lastBlindTapTime = 0L
    private val blindTapCooldownMs = 2500L

    // Teks yang dicari dengan "mengandung" (substring, tidak case-sensitive)
    private val skipKeywords = listOf(
        "Skip", "Lewati", "Tutup", "Close",
        "Skip Ad", "Skip Advertisement", "Lewati Iklan",
        "Lanjut", "Selanjutnya", "Continue", "Next"
    )

    // Simbol yang dicari dengan PERSIS SAMA (exact match)
    private val skipSymbols = listOf(
        "X", "x", "✕", "✖", "×",
        ">>>", ">>", ">",
        "⏭", "⏭️", "▶|", "▶️|"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(OverlayService.PREFS_NAME, MODE_PRIVATE)

        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopService(Intent(this, OverlayService::class.java))
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (::prefs.isInitialized && !prefs.getBoolean(OverlayService.KEY_ENABLED, false)) return

        val rootNode = rootInActiveWindow ?: return

        if (isProcessingClick) return

        if (findAndClickByText(rootNode)) return
        if (findAndClickBySymbol(rootNode)) return
        if (findAndClickIcon(rootNode)) return

        performBlindCornerTapsIfNeeded()
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo): Boolean {
        for (keyword in skipKeywords) {
            val nodes = node.findAccessibilityNodeInfosByText(keyword)
            if (nodes != null) {
                for (n in nodes) {
                    if (clickNodeOrParent(n)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun findAndClickBySymbol(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        for (symbol in skipSymbols) {
            if ((text != null && text == symbol) || (desc != null && desc == symbol)) {
                if (clickNodeOrParent(node)) return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (findAndClickBySymbol(childNode)) {
                    return true
                }
            }
        }

        return false
    }

    private fun findAndClickIcon(node: AccessibilityNodeInfo): Boolean {
        val description = node.contentDescription?.toString()?.lowercase()
        if (description != null && (
            description.contains("close") ||
            description.contains("tutup") ||
            description.contains("skip") ||
            description.contains("lewati") ||
            description.contains("next") ||
            description.contains("forward") ||
            description.contains("lanjut") ||
            description.contains("resume") ||
            description.contains("play")
        )) {
            if (clickNodeOrParent(node)) return true
        }

        val className = node.className?.toString()
        if (className == "android.widget.ImageView" || className == "android.widget.ImageButton") {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val width = bounds.width()
            val height = bounds.height()

            // Rentang diperlebar (dulu 10..180) supaya icon skip/next yang
            // bentuknya lebih besar (lingkaran ▶|) ikut tertangkap
            if (width in 10..220 && height in 10..220) {
                if (node.isClickable || (node.parent != null && node.parent.isClickable)) {
                    if (clickNodeOrParent(node)) return true
                }
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (findAndClickIcon(childNode)) {
                    return true
                }
            }
        }

        return false
    }

    private fun performBlindCornerTapsIfNeeded() {
        if (!::prefs.isInitialized) return
        if (!prefs.getBoolean(OverlayService.KEY_AGGRESSIVE, false)) return

        val now = System.currentTimeMillis()
        if (now - lastBlindTapTime < blindTapCooldownMs) return
        lastBlindTapTime = now

        val metrics = resources.displayMetrics
        val marginPx = (28 * metrics.density)
        val topY = marginPx
        val topRightX = metrics.widthPixels - marginPx
        val topLeftX = marginPx

        dispatchTap(topRightX, topY)
        Handler(Looper.getMainLooper()).postDelayed({
            dispatchTap(topLeftX, topY)
        }, 350)
    }

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var currentNode: AccessibilityNodeInfo? = node

        while (currentNode != null) {
            if (currentNode.isClickable) {
                isProcessingClick = true
                val targetNode = currentNode

                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                Handler(Looper.getMainLooper()).postDelayed({
                    isProcessingClick = false
                }, 1000)

                return true
            }
            currentNode = currentNode.parent
        }

        return false
    }

    override fun onInterrupt() {
        isProcessingClick = false
    }
}