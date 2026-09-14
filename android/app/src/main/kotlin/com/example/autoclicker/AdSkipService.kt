package com.example.auto

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipService : AccessibilityService() {

    private var isProcessingClick = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        if (isProcessingClick) return

        if (findAndClickByText(rootNode)) return
        findAndClickIcon(rootNode)
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo): Boolean {
        val skipKeywords = listOf(
            "Skip", "Lewati", "Tutup", "Close", 
            "Skip Ad", "Skip Advertisement", "Lewati Iklan"
        )

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

    private fun findAndClickIcon(node: AccessibilityNodeInfo): Boolean {
        val description = node.contentDescription?.toString()?.lowercase()
        if (description != null && (
            description.contains("close") || 
            description.contains("tutup") || 
            description.contains("skip") || 
            description.contains("lewati")
        )) {
            if (clickNodeOrParent(node)) return true
        }

        val className = node.className?.toString()
        if (className == "android.widget.ImageView" || className == "android.widget.ImageButton") {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val width = bounds.width()
            val height = bounds.height()

            if (width in 10..180 && height in 10..180) {
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