package cc.aoeiuv020.feedback.server

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * Created by AoEiuV020 on 2018.03.25-22:19:12.
 */
class FeedbackServer(inetSocketAddress: InetSocketAddress) : WebSocketServer(inetSocketAddress) {
    companion object {
        private val aId: AtomicInteger = AtomicInteger(1)
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val mainThread = Thread.currentThread() as Object

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val id = aId.getAndIncrement()
        conn.send("Welcome! you are user $id,")
        conn.setAttachment(id)
        broadcast("new user: $id,")
        local {
            "${Date()}: new user: $id,"
        }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        val id = conn.getAttachment<Int>()
        val uMessage = id.toString() + " has left the room!"
        broadcast(uMessage)
        local {
            uMessage
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val id = conn.getAttachment<Int>()
        val uMessage = "$id: $message"
        broadcast(uMessage)
        local {
            uMessage
        }
    }

    override fun onStart() {
        local {
            "Server started! $address"
        }
        synchronized(mainThread) {
            mainThread.notify()
        }
    }

    /**
     * 这个conn可能空，
     */
    override fun onError(conn: WebSocket?, ex: Exception) {
        ex.printStackTrace()
    }

    private fun local(message: () -> String) {
        System.out.println(message())
    }
}