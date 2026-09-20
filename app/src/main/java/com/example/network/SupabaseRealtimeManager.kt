package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class RealtimeNotification(
    val id: String,
    val recipientId: String,
    val actorId: String,
    val type: String,
    val postId: String?,
    val isRead: Boolean,
    val createdAt: Long
)

/**
 * Manages the real-time Supabase WebSocket connection using Phoenix Channels.
 * Listens for INSERT events on the public.notifications table and emits them to Kotlin flows.
 */
class SupabaseRealtimeManager(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val HEARTBEAT_INTERVAL_MS = 25_000L
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val messageRef = AtomicInteger(1)
    private var currentUserId: String? = null

    private val _notifications = MutableSharedFlow<RealtimeNotification>(extraBufferCapacity = 64)
    val notifications: SharedFlow<RealtimeNotification> = _notifications.asSharedFlow()

    private var isConnected = false
    private var reconnectBackoffMs = 2000L

    fun connect(userId: String) {
        currentUserId = userId
        if (isConnected || webSocket != null) return

        val wsUrl = SupabaseConfig.URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/realtime/v1/websocket?apikey=${SupabaseConfig.ANON_KEY}&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Supabase Realtime WebSocket")
                isConnected = true
                reconnectBackoffMs = 2000L

                // 1. Join notifications channel
                val ref = messageRef.getAndIncrement().toString()
                val joinMsg = JSONObject().apply {
                    put("topic", "realtime:public:notifications")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            val changes = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "INSERT")
                                    put("schema", "public")
                                    put("table", "notifications")
                                })
                            }
                            put("postgres_changes", changes)
                        })
                    })
                    put("ref", ref)
                }
                ws.send(joinMsg.toString())

                // 2. Start heartbeat job
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")

                    if (event == "postgres_changes") {
                        val payload = json.optJSONObject("payload")
                        val data = payload?.optJSONObject("data")
                        val record = data?.optJSONObject("record")

                        if (record != null) {
                            val recipientId = record.optString("recipient_id")
                            // Filter for current user
                            if (currentUserId.isNullOrBlank() || recipientId == currentUserId) {
                                val item = RealtimeNotification(
                                    id = record.optString("id"),
                                    recipientId = recipientId,
                                    actorId = record.optString("actor_id"),
                                    type = record.optString("type", "like"),
                                    postId = record.optString("post_id").takeIf { it.isNotBlank() },
                                    isRead = record.optBoolean("is_read", false),
                                    createdAt = System.currentTimeMillis()
                                )
                                Log.d(TAG, "Received real-time notification: ${item.type} from ${item.actorId}")
                                _notifications.tryEmit(item)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing realtime message", e)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                isConnected = false
                webSocket = null
                stopHeartbeat()
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                isConnected = false
                webSocket = null
                stopHeartbeat()
                scheduleReconnect()
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && isConnected) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    val heartbeat = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", messageRef.getAndIncrement().toString())
                    }
                    webSocket?.send(heartbeat.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat failed", e)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        if (!scope.isActive || currentUserId == null) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(reconnectBackoffMs)
            reconnectBackoffMs = (reconnectBackoffMs * 2).coerceAtMost(30_000L)
            Log.d(TAG, "Attempting reconnect to Realtime...")
            currentUserId?.let { connect(it) }
        }
    }

    fun disconnect() {
        currentUserId = null
        stopHeartbeat()
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "App closed")
        } catch (ignored: Exception) {}
        webSocket = null
        isConnected = false
    }
}
