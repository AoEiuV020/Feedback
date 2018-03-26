package cc.aoeiuv020.feedback

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.startService
import java.lang.Exception
import java.net.URI
import java.net.URISyntaxException

class ListenFeedbackService : Service(), AnkoLogger {
    companion object {
        var isRunning: Boolean = false
        private var url: String = "ws://feedback.aoeiuv020.cc/ws"
        private var client: WebSocketClient? = null
        private val NOTIFICATION_ID = ListenFeedbackService::class.java.simpleName.hashCode()
        fun start(ctx: Context, url: String) {
            ctx.startService<ListenFeedbackService>("url" to url)
        }

        fun stop(ctx: Context) {
            ctx.stopService(ctx.intentFor<ListenFeedbackService>())
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        try {
            client = Client(URI(url)).apply {
                connect()
            }
        } catch (e: URISyntaxException) {
            notify("bad url: $url")
            throw e
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        client?.close()
        client = null
        super.onDestroy()
    }

    inner class Client(uri: URI) : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            runOnUiThread {
                notify("connected,")
            }
        }

        override fun onMessage(message: String) {
            runOnUiThread {
                notify(message)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            runOnUiThread {
                notify("closed, $reason")
            }
        }

        override fun onError(ex: Exception?) {
            runOnUiThread {
                notify("error: ${ex?.message}")
            }
        }
    }

    fun notify(text: String? = null, title: String? = null, noCancel: Boolean = false) {
        val icon = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            android.R.drawable.ic_dialog_info
        } else {
            R.drawable.ic_message
        }
        val channelId = "channel_default"
        val name = "default"
        val pi = PendingIntent.getActivity(this, 0, intentFor<FeedbackActivity>(), 0)
        val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setSmallIcon(icon)
                .build()
        if (noCancel) {
            notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        }
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 外面包了个判断还是有警告只能再包一层，
                    NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
                } else {
                    throw RuntimeException("这不可能，")
                }
                manager.createNotificationChannel(channel)
            }
        }
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }

}
