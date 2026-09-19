package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Classification of network connection types.
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OFFLINE,
    UNKNOWN
}

/**
 * Sync strategy based on connectivity.
 */
enum class SyncStrategy {
    /** Full unmetered sync (Wi-Fi, Ethernet). Large PDF downloads and uploads permitted. */
    IMMEDIATE_FULL_SYNC,
    /** Metered sync (Cellular mobile data). Previews loaded, large PDF streams require confirmation. */
    METERED_DATA_SAVER,
    /** Zero connection. Room v5 local SQLite database used as source of truth. Zero network calls. */
    LOCAL_ROOM_FALLBACK
}

/**
 * Current network status model.
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val isMetered: Boolean,
    val isHighBandwidth: Boolean,
    val offlineFallbackActive: Boolean
) {
    companion object {
        val OFFLINE = NetworkStatus(
            isConnected = false,
            connectionType = ConnectionType.OFFLINE,
            isMetered = false,
            isHighBandwidth = false,
            offlineFallbackActive = true
        )

        val WIFI = NetworkStatus(
            isConnected = true,
            connectionType = ConnectionType.WIFI,
            isMetered = false,
            isHighBandwidth = true,
            offlineFallbackActive = false
        )

        val CELLULAR = NetworkStatus(
            isConnected = true,
            connectionType = ConnectionType.CELLULAR,
            isMetered = true,
            isHighBandwidth = false,
            offlineFallbackActive = false
        )
    }

    /**
     * Determines whether large transfers (e.g. manuscripts > 15 MB) should proceed immediately.
     */
    fun canPerformLargeTransfer(fileSizeBytes: Long, thresholdBytes: Long = 15 * 1024 * 1024L): Boolean {
        if (!isConnected) return false
        if (connectionType == ConnectionType.WIFI || connectionType == ConnectionType.ETHERNET) return true
        // On cellular, restrict if above threshold to prevent carrier bill shock
        return fileSizeBytes <= thresholdBytes
    }

    /**
     * Returns appropriate synchronization strategy for feed and vault operations.
     */
    fun getSyncStrategy(): SyncStrategy {
        return when {
            !isConnected -> SyncStrategy.LOCAL_ROOM_FALLBACK
            connectionType == ConnectionType.WIFI || connectionType == ConnectionType.ETHERNET -> SyncStrategy.IMMEDIATE_FULL_SYNC
            connectionType == ConnectionType.CELLULAR -> SyncStrategy.METERED_DATA_SAVER
            else -> SyncStrategy.METERED_DATA_SAVER
        }
    }
}

/**
 * Evaluates raw network capability flags into high-level NetworkStatus.
 * Pure logic decoupled from Android OS for deterministic testing in CI/CD.
 */
object NetworkCapabilityEvaluator {

    fun evaluate(
        hasInternet: Boolean,
        isWifi: Boolean,
        isCellular: Boolean,
        isEthernet: Boolean = false,
        isNotMetered: Boolean = true
    ): NetworkStatus {
        if (!hasInternet) {
            return NetworkStatus.OFFLINE
        }

        val type = when {
            isWifi -> ConnectionType.WIFI
            isCellular -> ConnectionType.CELLULAR
            isEthernet -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }

        val isMetered = !isNotMetered || isCellular
        val isHighBandwidth = (isWifi || isEthernet) && !isMetered

        return NetworkStatus(
            isConnected = true,
            connectionType = type,
            isMetered = isMetered,
            isHighBandwidth = isHighBandwidth,
            offlineFallbackActive = false
        )
    }
}

/**
 * Singleton manager observing active Android connectivity.
 */
class NetworkConnectionManager(private val context: Context? = null) {

    private val _statusFlow = MutableStateFlow(NetworkStatus.WIFI)
    val statusFlow: StateFlow<NetworkStatus> = _statusFlow.asStateFlow()

    val currentStatus: NetworkStatus
        get() = _statusFlow.value

    init {
        context?.let { startMonitoring(it) }
    }

    fun setStatusForTesting(status: NetworkStatus) {
        _statusFlow.value = status
    }

    private fun startMonitoring(ctx: Context) {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateFromActiveNetwork(connectivityManager)
            }

            override fun onLost(network: Network) {
                updateFromActiveNetwork(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val isEthernet = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                val isNotMetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

                _statusFlow.value = NetworkCapabilityEvaluator.evaluate(
                    hasInternet = hasInternet,
                    isWifi = isWifi,
                    isCellular = isCellular,
                    isEthernet = isEthernet,
                    isNotMetered = isNotMetered
                )
            }
        })

        updateFromActiveNetwork(connectivityManager)
    }

    private fun updateFromActiveNetwork(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)

        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            _statusFlow.value = NetworkStatus.OFFLINE
            return
        }

        _statusFlow.value = NetworkCapabilityEvaluator.evaluate(
            hasInternet = true,
            isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            isNotMetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        )
    }
}
