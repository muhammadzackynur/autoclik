package com.example.auto

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.auto/accessibility"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "openAccessibilitySettings" -> {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    result.success(true)
                }
                "isAccessibilityServiceEnabled" -> {
                    result.success(isAccessibilityServiceEnabled())
                }
                "checkOverlayPermission" -> {
                    result.success(Settings.canDrawOverlays(this))
                }
                "requestOverlayPermission" -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    result.success(true)
                }
                "startOverlayService" -> {
                    startService(Intent(this, OverlayService::class.java))
                    result.success(true)
                }
                "stopOverlayService" -> {
                    stopService(Intent(this, OverlayService::class.java))
                    result.success(true)
                }
                "setAggressiveMode" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    getSharedPreferences(OverlayService.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(OverlayService.KEY_AGGRESSIVE, enabled)
                        .apply()
                    result.success(true)
                }
                "getAggressiveMode" -> {
                    val enabled = getSharedPreferences(OverlayService.PREFS_NAME, MODE_PRIVATE)
                        .getBoolean(OverlayService.KEY_AGGRESSIVE, false)
                    result.success(enabled)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/.AdSkipService"
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        for (componentName in enabledServicesSetting.split(":")) {
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}