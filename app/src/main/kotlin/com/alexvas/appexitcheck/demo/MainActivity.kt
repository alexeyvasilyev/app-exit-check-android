package com.alexvas.appexitcheck.demo

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.alexvas.appexitcheck.AppExitCheck
import com.alexvas.appexitcheck.AppExitCheck.AppExitCheckListener
import com.alexvas.appexitcheck.AppExitCheck.Companion.TAG
import java.lang.Exception
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private var appExitCheck: AppExitCheck? = null
    private var leakTimer: Timer? = null
    private var button: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.button)
        button!!.setOnClickListener {
            leakTimer = Timer("LeakThread")
            leakTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    try {
                        val url = URL("https://google.com")
                        val connection: HttpsURLConnection =
                            url.openConnection() as HttpsURLConnection
                        // Do nothing. HTTPS negotiation already increased traffic consumption
                        Log.i(TAG, "$url response code ${connection.responseCode}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 0,5000L)
            button!!.isEnabled = false
        }

        AppExitCheck.Builder(this)
            .withThreshold(100)
            .withTimeout(5000)
            .withInterval(2000)
            .withListener(object : AppExitCheckListener {
                override fun onFailedLeakedTrafficDetected(leakedBytes: Long) {
                    showLeaksNotification(
                        "App leaked traffic detected",
                        "Consumed $leakedBytes bytes within 2 sec after app closed."
                    )
                }

                override fun onSuccess() {
                    Log.i(TAG, "No leaks detected")
                }
            })
            .build().also { appExitCheck = it }
        createNotifications()
    }

    private fun createNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelDefault = NotificationChannel(
                "channel_debug",
                "Debug",
                NotificationManager.IMPORTANCE_LOW
            )
            channelDefault.setShowBadge(false)
//          channelDefault.setLightColor(Color.RED); //小红点颜色
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                channelDefault
            )
        }
    }


    @SuppressLint("InlinedApi")
    private fun showLeaksNotification(
        title: String,
        text: String
    ) {
        val nm = getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = Intent(Intent.ACTION_MAIN)
        contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendContentIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, "channel_debug")
                .setSmallIcon(R.drawable.ic_alert_circle_black_24dp)
                .setContentIntent(pendContentIntent)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setColor(Color.RED)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Show new notification icon
        nm.notify(54321, builder.build())
    }

    override fun onResume() {
        super.onResume()
        appExitCheck?.cancelAppExitCheck()
        button!!.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        appExitCheck?.scheduleAppExitCheck()
    }

}