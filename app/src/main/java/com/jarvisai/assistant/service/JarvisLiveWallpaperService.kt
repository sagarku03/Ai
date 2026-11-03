package com.jarvisai.assistant.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Interactive Android Live Wallpaper rendering the JARVIS Arc Reactor core on the user's
 * home screen, reacting to touch ripples and active `LiveVoiceSessionBridge` state.
 */
class JarvisLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = JarvisReactorWallpaperEngine()

    private inner class JarvisReactorWallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var isVisibleOnScreen = false
        private var phaseAngle = 0f
        private var touchPulse = 0f
        private var touchX = -1f
        private var touchY = -1f

        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.parseColor("#22D3EE")
        }

        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisibleOnScreen) {
                    handler.postDelayed(this, 33L)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisibleOnScreen = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_MOVE) {
                touchX = event.x
                touchY = event.y
                touchPulse = 1f
            }
            super.onTouchEvent(event)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            isVisibleOnScreen = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val baseRadius = min(w, h) * 0.24f

                canvas.drawColor(Color.parseColor("#060810"))

                val isVoiceActive = LiveVoiceSessionBridge.isSessionActive.value
                val primaryColor = if (isVoiceActive) {
                    Color.parseColor("#A3FF12")
                } else {
                    Color.parseColor("#22D3EE")
                }

                phaseAngle = (phaseAngle + if (isVoiceActive) 4.2f else 1.5f) % 360f
                touchPulse = (touchPulse * 0.90f).coerceAtLeast(0f)

                val pulseRadius = baseRadius * (1f + 0.08f * sin(Math.toRadians(phaseAngle.toDouble())).toFloat() + touchPulse * 0.15f)

                corePaint.shader = RadialGradient(
                    cx,
                    cy,
                    pulseRadius * 1.4f,
                    intArrayOf(primaryColor, Color.parseColor("#3B82F6"), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy, pulseRadius * 1.4f, corePaint)

                ringPaint.color = primaryColor
                canvas.drawCircle(cx, cy, baseRadius * 0.85f, ringPaint)

                for (i in 0 until 3) {
                    val rad = Math.toRadians((phaseAngle + i * 120f).toDouble())
                    val nx = cx + (baseRadius * 0.85f * cos(rad)).toFloat()
                    val ny = cy + (baseRadius * 0.85f * sin(rad)).toFloat()
                    ringPaint.style = Paint.Style.FILL
                    canvas.drawCircle(nx, ny, 8f, ringPaint)
                    ringPaint.style = Paint.Style.STROKE
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
