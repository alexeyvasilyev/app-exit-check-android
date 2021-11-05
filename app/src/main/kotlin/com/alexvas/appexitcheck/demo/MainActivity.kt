package com.alexvas.appexitcheck.demo

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import com.alexvas.appexitcheck.AppExitCheck
import com.alexvas.appexitcheck.AppExitCheck.AppExitCheckListener
import com.alexvas.appexitcheck.AppExitCheck.Companion.TAG
import java.lang.Exception
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import androidx.core.app.NotificationManagerCompat

import android.app.NotificationManager


class MainActivity : AppCompatActivity() {

    private var appExitCheck: AppExitCheck? = null
    private var leakTimer: Timer? = null
    private var button: Button? = null
    private val ACTION_KILL = "android.intent.action.KILL"

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
                        connection.inputStream.close()
                        connection.disconnect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 0,5000L)
            button!!.isEnabled = false
        }

        createNotificationChannel()

        AppExitCheck.Builder(this)
            // Log debug data on logcat
            .withDebug(true)
            // Show warning only if at least 100 bytes leaked
            .withThreshold(100)
            // Start checking after 2 sec app closed
            .withTimeout(2000)
            // Measure traffic consumption within 5 sec
            .withInterval(5000)
            .withListener(object : AppExitCheckListener {
                override fun onFailedLeakedTrafficDetected(leakedBytes: Long) {
                    showLeaksNotification(
                        "App leaked traffic detected",
                        "Consumed $leakedBytes bytes within 5 sec after app closed."
                    )
                }

                override fun onSuccess() {
                    Log.i(TAG, "No leaks detected")
                }
            })
            .build().also { appExitCheck = it }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannelCompat.Builder("channel_debug", NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Debug")
                .build()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(channel)
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
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val killIntent = Intent(this, MainActivity::class.java)
        killIntent.action = ACTION_KILL

        val killPendingIntent = PendingIntent.getActivity(
            this,
            0,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE)

        builder.addAction(
            R.drawable.ic_alert_circle_black_24dp,
            "Kill app",
            killPendingIntent)

        // Show new notification icon
        nm.notify(54321, builder.build())
    }

    private fun killApp() {
        // Kill the current process
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onResume() {
        super.onResume()
        appExitCheck?.cancelAppExitCheck()
        button!!.isEnabled = true

        if (intent.action == ACTION_KILL) {
            finish()
            killApp()
        }
    }

    override fun onPause() {
        super.onPause()
        appExitCheck?.scheduleAppExitCheck()
    }

}