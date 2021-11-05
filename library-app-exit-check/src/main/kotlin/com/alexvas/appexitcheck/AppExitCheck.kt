package com.alexvas.appexitcheck

import android.app.*

import android.content.Context
import android.net.TrafficStats
import android.util.Log
import java.util.*


class AppExitCheck private constructor(
    private val context: Context,
    private val listener: AppExitCheckListener?,
    private val timeout: Int,
    private val interval: Int,
    private val threshold: Int,
    private val debug: Boolean) {

    private var postCheckTimer: Timer? = null
    private var lastRxBytesObtained: Long = 0

    companion object {
        const val DEBUG = false
        val TAG: String = AppExitCheck::class.java.simpleName
    }

    interface AppExitCheckListener {
        fun onFailedLeakedTrafficDetected(leakedBytes: Long)
        fun onSuccess()
    }

    class Builder(private val context: Context) {
        private var timeout:   Int = 5000 // msec
        private var interval:  Int = 5000 // msec
        private var threshold: Int = 0    // bytes
        private var debug: Boolean = false
        private var listener: AppExitCheckListener? = null

        /**
         * Timeout (msec) specifies when the check should be done after
         * scheduleAppExitCheck() method called.
         * By default, 5000.
         */
        fun withTimeout(value: Int): Builder {
            this.timeout = value
            return this
        }

        /**
         * Interval (msec) specifies for how long measure traffic consumption.
         * By default, 5000.
         */
        fun withInterval(value: Int): Builder {
            this.interval = value
            return this
        }

        /**
         * Threshold (bytes) specifies how many bytes are allowed to be consumed before
         * onFailedLeakedTrafficDetected(leakedBytes) callback called.
         * By default, 0.
         */
        fun withThreshold(value: Int): Builder {
            this.threshold = value
            return this
        }

        /**
         * Show debug info on logcat.
         * By default, false.
         */
        fun withDebug(value: Boolean): Builder {
            this.debug = value
            return this
        }

        /**
         * Callback to be called when leaked bytes detected.
         */
        fun withListener(value: AppExitCheckListener?): Builder {
            this.listener = value
            return this
        }

        fun build(): AppExitCheck {
            return AppExitCheck(context, listener, timeout, interval, threshold, debug)
        }
    }

    /**
     * Schedule traffic consumption check.
     */
    fun scheduleAppExitCheck() {
        cancelAppExitCheck()
        lastRxBytesObtained = 0

        if (DEBUG) Log.v(TAG, "scheduleAppExitCheck()")
        if (postCheckTimer == null) postCheckTimer = Timer("AppExitCheckTimer")

        // Create a separate thread with 2 tasks.
        // 1. Save last consumed traffic state.
        postCheckTimer!!.schedule(object : TimerTask() {
            override fun run() {
                lastRxBytesObtained = getRxBytes()
                if (debug) Log.d(TAG, "Total RX bytes 1: $lastRxBytesObtained")
            }
        }, timeout + 0L)

        // 2. Check if network traffic is still consumed.
        postCheckTimer!!.schedule(object : TimerTask() {
            override fun run() {
                checkTrafficConsumption()
                // Cleanup timer
                cancelAppExitCheck()
            }
        }, (timeout + interval).toLong())
    }

    /**
     * Cancel traffic consumption check.
     */
    fun cancelAppExitCheck() {
        if (DEBUG) Log.v(TAG, "cancelAppExitCheck()")
        postCheckTimer?.cancel()
        postCheckTimer = null
    }

    private fun checkTrafficConsumption() {
        if (DEBUG) Log.v(TAG, "checkTrafficConsumption()")
        val rx = getRxBytes()
        if (debug) Log.d(TAG, "Total RX bytes 2: $rx")
        val consumedBytes: Long = rx - lastRxBytesObtained
        if (consumedBytes - threshold > 0) {
            if (debug) Log.i(TAG, "Traffic consumed within $interval msec $consumedBytes bytes")
            listener?.onFailedLeakedTrafficDetected(consumedBytes)
            lastRxBytesObtained = rx
        } else {
            if (debug) Log.i(TAG, "No traffic consumption detected")
            listener?.onSuccess()
        }
    }

    /**
     * Get number of bytes received for the current package.
     */
    private fun getRxBytes(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses
        if (processes != null) {
            for (process in processes) {
                if (context.packageName.equals(process.processName, ignoreCase = true)) {
                    // Suppose app has only 1 process
                    return TrafficStats.getUidRxBytes(process.uid)
                }
            }
        }
        return 0
    }

}