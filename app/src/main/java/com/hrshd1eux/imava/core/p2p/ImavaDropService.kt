package com.hrshd1eux.imava.core.p2p

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.ui.MainActivity

class ImavaDropService : Service() {

    companion object {
        const val CHANNEL_ID = "imava_drop_transfer_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.hrshd1eux.imava.p2p.START"
        const val ACTION_STOP = "com.hrshd1eux.imava.p2p.STOP"

        fun start(context: Context) {
            val intent = Intent(context, ImavaDropService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ImavaDropService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ImavaDropServer.stop(stopService = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val targetActivity = if (ImavaDropServer.launchedFromExternalShare) {
            com.hrshd1eux.imava.ui.p2p.ImavaDropShareActivity::class.java
        } else {
            MainActivity::class.java
        }
        val openAppIntent = Intent(this, targetActivity).apply {
            setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ImavaDropService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locCtx = com.hrshd1eux.imava.core.util.LocaleHelper.localizedContext(this)
        val url = ImavaDropServer.state.value.url ?: locCtx.getString(com.hrshd1eux.imava.R.string.drop_notif_fallback_wifi)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(locCtx.getString(com.hrshd1eux.imava.R.string.drop_notif_title))
            .setContentText(locCtx.getString(com.hrshd1eux.imava.R.string.drop_notif_text_fmt, url))
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, locCtx.getString(com.hrshd1eux.imava.R.string.drop_stop_sharing), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (serviceType != 0) {
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val locCtx = com.hrshd1eux.imava.core.util.LocaleHelper.localizedContext(this)
            val channel = NotificationChannel(
                CHANNEL_ID,
                locCtx.getString(com.hrshd1eux.imava.R.string.drop_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = locCtx.getString(com.hrshd1eux.imava.R.string.drop_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
