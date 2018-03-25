package cc.aoeiuv020.feedback.server

import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * 程序入口，
 * Created by AoEiuV020 on 2018.03.26-01:21:19.
 */
object App {
    private lateinit var server: FeedbackServer
    @JvmStatic
    fun main(args: Array<String>) {
        server = when (args.size) {
            1 -> {
                try {
                    val port = args[0].toInt()
                    FeedbackServer(InetSocketAddress(port)).apply {
                        start()
                    }
                } catch (e: NumberFormatException) {
                    System.err.println("illegal port ${args[0]}")
                    return
                }
            }
            2 -> {
                try {
                    val port = args[1].toInt()
                    val ip = InetAddress.getByName(args[0])
                    FeedbackServer(InetSocketAddress(ip, port)).apply {
                        start()
                    }
                } catch (e: NumberFormatException) {
                    System.err.println("illegal port ${args[1]}")
                    return
                }
            }
            else -> {
                usage()
                return
            }
        }
        readCommand()
    }

    private fun readCommand() {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val mainThread = Thread.currentThread() as Object
        synchronized(mainThread) {
            try {
                mainThread.wait()
            } catch (_: InterruptedException) {
            }
        }
        try {
            var line = readLine()
            while (line != null) {
                server.broadcast("server: $line")
                if (line == "quit") {
                    server.stop(TimeUnit.SECONDS.toMillis(1).toInt())
                    break
                }
                line = readLine()
            }
        } catch (e: Exception) {
            System.err.println(e.message)
            server.stop(TimeUnit.SECONDS.toMillis(1).toInt())
        }
        System.err.println("stop read,")
    }

    private fun usage() {
        System.err.println("FeedbackServer [<ip>] <port>")
    }
}