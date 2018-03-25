package cc.aoeiuv020.feedback

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.feedback_activity_client.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.error
import org.jetbrains.anko.startActivity
import java.lang.Exception
import java.net.URI
import java.net.URISyntaxException

class FeedbackActivity : AppCompatActivity(), AnkoLogger {
    companion object {
        fun start(ctx: Context) {
            ctx.startActivity<FeedbackActivity>()
        }
    }

    var webSocket: WebSocketClient? = null
    val wsUrl: String? get() = webSocket?.remoteSocketAddress?.toString()
    val urlSet = mutableSetOf<String>()
    lateinit var urlAdapter: ArrayAdapter<String>
    val URL_KEY_NAME = "url_key_name"
    val messageListAdapter = MessageListAdapter(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feedback_activity_client)

        getPreferences(Context.MODE_PRIVATE).getStringSet(URL_KEY_NAME, setOf<String>()).let {
            urlSet.addAll(it)
        }
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlSet.toMutableList()).let {
            urlAdapter = it
            editTextUrl.setAdapter(it)
        }
        listView.adapter = messageListAdapter

        beforeConnect()
    }

    override fun onPause() {
        super.onPause()
        debug { "save url autocomplete list <${urlSet.size}>" }
        getPreferences(Context.MODE_PRIVATE).edit()
                .putStringSet(URL_KEY_NAME, urlSet)
                .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "destroy")
    }

    @Synchronized
    fun addMessage(message: Message) {
        debug { "add message <$message>" }
        messageListAdapter.add(message)
        listView.apply {
            post {
                setSelection(adapter.count - 1)
            }
        }
    }

    fun beforeConnect() {
        debug { "beforeConnect" }
        buttonConnect.apply {
            isClickable = true
            text = getString(R.string.connect)
            setOnClickListener {
                connecting(editTextUrl.text.toString().takeIf(String::isNotEmpty)
                        ?: getString(R.string.ws_url_default)
                )
            }
        }
        buttonSend.apply {
            isClickable = false
            setColorFilter(color(R.color.sendDisable))
        }
    }

    fun connecting(str: String) {
        debug { "connecting $str" }
        urlAdapter.takeUnless { str in urlSet }?.apply {
            add(str)
            urlSet.add(str)
        }
        buttonConnect.apply {
            isClickable = false
            text = getString(R.string.connecting)
        }
        buttonSend.apply {
            isClickable = false
            setColorFilter(color(R.color.sendDisable))
        }
        try {
            webSocket = Client(URI(str)).apply {
                connect()
            }
        } catch (e: URISyntaxException) {
            addMessage(Message("Illegal uri ${e.message}", MessageType.LOG))
        }
    }

    fun connected() {
        debug { "connected $wsUrl" }
        addMessage(Message("${wsUrl} connected", MessageType.LOG))
        buttonConnect.apply {
            isClickable = true
            text = getString(R.string.close)
            setOnClickListener {
                webSocket?.close(1000, "user close")
                        ?: closed()
            }
        }
        buttonSend.apply {
            isClickable = true
            setColorFilter(color(R.color.sendEnable))
            setOnClickListener {
                editTextMessage.text.toString().takeIf(String::isNotEmpty)?.let {
                    addMessage(Message(it, MessageType.ME))
                    webSocket?.send(it)
                            ?: closed()
                    editTextMessage.setText("")
                }
            }
        }
        editTextMessage.requestFocus()
    }

    fun message(text: String) {
        addMessage(Message(text, MessageType.OTHER))
    }

    @Synchronized
    fun closed() {
        if (webSocket == null) {
            return
        }
        debug { "closed $wsUrl" }
        addMessage(Message("${wsUrl} closed", MessageType.LOG))
        webSocket = null
        beforeConnect()
    }

    fun failure(ex: Exception?) {
        error("failure", ex)
        addMessage(Message("error: ${ex?.message}", MessageType.LOG))
        closed()
    }

    inner class Client(uri: URI) : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            runOnUiThread {
                connected()
            }
        }

        override fun onMessage(message: String?) {
            runOnUiThread {
                message?.let { message(it) }
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            close(1000, "closing")
            runOnUiThread {
                closed()
            }
        }

        override fun onError(ex: Exception?) {
            runOnUiThread {
                failure(ex)
            }
        }
    }
}
