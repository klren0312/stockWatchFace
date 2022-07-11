package com.zzes.stock2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.palette.graphics.Palette
import com.zzes.stock2.beans.SinaStockBean
import com.zzes.stock2.beans.SinaStockBeanItem
import com.zzes.stock2.beans.WatchFaceData
import okhttp3.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit

private const val SHADOW_RADIUS = 6f

/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

// 股票数据标志
private const val STOCK_MSG_UPDATE = 1


/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */

class MyWatchFace : CanvasWatchFaceService() {
    companion object {
        private var stockData = SinaStockBean()
    }
    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: Engine) : Handler(Looper.myLooper()!!) {
        private val mWeakReference: WeakReference<Engine> = WeakReference(reference)
        // 处理子进程消息
        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                    STOCK_MSG_UPDATE -> {
                        val stockArr = msg.obj.toString().split("\n")
                        stockData.clear()
                        stockArr.forEach {
                            if (it != "") {
                                val content = it.split("=\"")
                                val arr = content[1].split(",")
                                Log.d("test", arr.toString())
                                if (arr.size > 3) {
                                    stockData.add(SinaStockBeanItem(arr[0], arr[1], arr[2], arr[3]))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode: Boolean = false
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F

        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private var mWatchHandColor: Int = 0
        private var mWatchHandShadowColor: Int = 0

        private lateinit var mStock1Paint: Paint

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            mCalendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            mBackgroundBitmap =
                BitmapFactory.decodeResource(resources, R.drawable.watchface_service_bg)

            /* Extracts colors from background image to improve watchface style. */
            Palette.from(mBackgroundBitmap).generate {
                it?.let {
                    updateWatchHandStyle()
                }
            }
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
            mStock1Paint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = 2f
                isAntiAlias = true
                style = Paint.Style.STROKE
                setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            updateWatchHandStyle()

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (mAmbient) {
                mStock1Paint.color = Color.WHITE
                mStock1Paint.isAntiAlias = false
                mStock1Paint.clearShadowLayer()
            } else {
                mStock1Paint.color = mWatchHandColor
                mStock1Paint.isAntiAlias = true
                mStock1Paint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f
            mCenterY = height / 2f

        }

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP ->
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(applicationContext, R.string.message, Toast.LENGTH_SHORT)
                        .show()
            }
            invalidate()
        }
        var time = 1
        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val watchFaceData = EditorActivity.watchFaceData
            Log.d("test", watchFaceData.stockCodes)
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)

            if (time % 20 == 0) {
                getSinaStockCata("sh000001,sz399001,sz399006,sh000688")
                time = 1
            } else {
                time += 1
            }
        }


        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }

        private fun drawWatchFace(canvas: Canvas) {
            var cd: Calendar = Calendar.getInstance()
            canvas.save()
            mStock1Paint.textSize = 20f
            mStock1Paint.textAlign = Paint.Align.CENTER
            mStock1Paint.color = Color.parseColor("#873800")
            Log.d("Calendar", Calendar.DAY_OF_WEEK.toString())
            var timeStr = addZero(mCalendar.get(Calendar.HOUR_OF_DAY)) + ":" + addZero(mCalendar.get(Calendar.MINUTE))
            canvas.drawText(timeStr, mCenterX, mCenterY / 7f, mStock1Paint)
            var dateStr = cd.get(Calendar.YEAR).toString() + "年" + (cd.get(Calendar.MONTH) + 1) + "月" + cd.get(Calendar.DATE) + "日"
            canvas.drawText(dateStr, mCenterX, mCenterY / 4f, mStock1Paint)
            canvas.restore()

            if (!stockData.isEmpty()) {
                var stockIndex = 0
                stockData.forEach {
                    canvas.save()
                    mStock1Paint.textAlign = Paint.Align.LEFT
                    if (it.currentChange.toDouble() > 0) {
                        mStock1Paint.color = Color.parseColor("#820014")
                    } else {
                        mStock1Paint.color = Color.parseColor("#135200")
                    }
                    Log.d("Height", (mCenterY / 8).toString())
                    canvas.drawText("${it.name}  ${it.current}  ${it.percent}%", mCenterX / 3f , mCenterY / 2f + stockIndex * (mCenterY / 7), mStock1Paint)
                    canvas.restore()

                    stockIndex++
                }
            }
        }

        private fun addZero(num: Int): String {
            if (num < 10) {
                return "0$num"
            } else {
                return num.toString()
            }
        }

        /**
         * 请求新浪股票接口
         */
        private fun getSinaStockCata(stockCode: String) {
            val TAG = "OKHTTP"
            var stockCodeArr: List<String> = listOf()
            if (stockCode != "") {
                stockCodeArr = stockCode.split(",")

                stockCodeArr = stockCodeArr.map { "s_${it}" }
            }
            Log.d(TAG, stockCodeArr.joinToString())

            val path = "http://hq.sinajs.cn/list=${stockCodeArr.joinToString(",")}"
            val okHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            var request = Request.Builder().url(path)
                .addHeader("Referer", "http://finance.sina.com.cn").build()

            val call: Call = okHttpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "onFailure: " + e.message)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "response.code():" + response.code)
                    mStock1Paint.textSize = 24f
                    mStock1Paint.textAlign = Paint.Align.CENTER
                    mStock1Paint.color = Color.parseColor("#E6A23C")
                    if (response.code === 200) {
                        val string: String? = response.body?.string()
                        if (string != null) {
                            Log.d(TAG, string)
                            val msg = mUpdateTimeHandler.obtainMessage();
                            //设置发送的内容
                            msg.what = STOCK_MSG_UPDATE
                            msg.obj = string
                            mUpdateTimeHandler.removeMessages(STOCK_MSG_UPDATE)
                            if (shouldTimerBeRunning()) {
                                mUpdateTimeHandler.sendMessage(msg)
                            }
                        }

                    }
                }
            })
        }
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren"t visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}


