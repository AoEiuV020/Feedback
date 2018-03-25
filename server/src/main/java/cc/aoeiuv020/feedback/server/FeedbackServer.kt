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

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val mainThread = Thread.currentThread() as Object
    private val nameProvider: NameProvider = NameProvider()

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val name = nameProvider.next()
        conn.send("Welcome! you are $name,")
        conn.setAttachment(name)
        broadcast("new user: $name,")
        local {
            "${Date()}: new user: $name,"
        }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        val name = conn.getAttachment<String>()
        val uMessage = "$name has left the room!"
        broadcast(uMessage)
        local {
            uMessage
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val name = conn.getAttachment<String>()
        val uMessage = "$name: $message"
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

    override fun stop(timeout: Int) {
        super.stop(timeout)

        nameProvider.close()
    }

    private fun local(message: () -> String) {
        System.out.println(message())
    }
}