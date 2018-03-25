package cc.aoeiuv020.feedback.server

import org.jsoup.Jsoup
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 *
 * Created by AoEiuV020 on 2018.03.26-04:55:50.
 */
class NameProvider : Runnable {
    private var running: Boolean = false
    @Suppress("PrivatePropertyName")
    private val DEFAULT_NAME: String = "无名氏"
    private val queue: BlockingQueue<String> = LinkedBlockingQueue<String>()
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val requestThread: Object = Thread(this, "requestUserName").apply { start() } as Object

    override fun run() {
        running = true
        while (running) {
            if (queue.size < 40) {
                requestNameList()
            }
            synchronized(requestThread) {
                requestThread.wait()
            }
        }
    }

    private fun requestNameList() {
        try {
            Jsoup.connect("http://www.xuanpai.com/tool/makers/5").get()
                    .select("#tool_item > ul > li")
                    .forEach {
                        queue.put(it.text())
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun next(): String {
        val name = queue.poll(2, TimeUnit.SECONDS)
        synchronized(requestThread) {
            requestThread.notify()
        }
        return name ?: DEFAULT_NAME
    }

    fun close() {
        running = false
        synchronized(requestThread) {
            requestThread.notify()
        }
    }
}