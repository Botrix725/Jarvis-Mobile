package com.botrix.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Opt-in accessibility foundation.
 * Android requires the user to enable this service manually in Settings.
 * This starter intentionally does not silently click or type into other apps.
 */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Observe only while enabled. */ }
    override fun onInterrupt() {}

    fun clickVisibleText(exactText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClick(root, exactText)
    }

    private fun findAndClick(node: AccessibilityNodeInfo, target: String): Boolean {
        val label = node.text?.toString() ?: node.contentDescription?.toString()
        if (label.equals(target, ignoreCase = true) && node.isEnabled && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child, target)) return true
        }
        return false
    }
}
