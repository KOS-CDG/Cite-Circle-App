package com.example.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying network connectivity classification, metered mobile data handling,
 * Wi-Fi unmetered transfers, and offline Room fallback strategies.
 */
class NetworkConnectionTest {

    @Test
    fun `wifi connection enables immediate full sync and unmetered transfers`() {
        val status = NetworkCapabilityEvaluator.evaluate(
            hasInternet = true,
            isWifi = true,
            isCellular = false,
            isNotMetered = true
        )

        assertEquals(ConnectionType.WIFI, status.connectionType)
        assertTrue(status.isConnected)
        assertFalse(status.isMetered)
        assertTrue(status.isHighBandwidth)
        assertFalse(status.offlineFallbackActive)

        assertEquals(SyncStrategy.IMMEDIATE_FULL_SYNC, status.getSyncStrategy())

        // Can upload/download large 50 MB manuscripts on Wi-Fi
        val fiftyMb = 50 * 1024 * 1024L
        assertTrue(status.canPerformLargeTransfer(fiftyMb))
    }

    @Test
    fun `cellular mobile data activates metered data saver strategy`() {
        val status = NetworkCapabilityEvaluator.evaluate(
            hasInternet = true,
            isWifi = false,
            isCellular = true,
            isNotMetered = false
        )

        assertEquals(ConnectionType.CELLULAR, status.connectionType)
        assertTrue(status.isConnected)
        assertTrue(status.isMetered)
        assertFalse(status.isHighBandwidth)
        assertFalse(status.offlineFallbackActive)

        assertEquals(SyncStrategy.METERED_DATA_SAVER, status.getSyncStrategy())

        // Small 2 MB paper is allowed on cellular
        val twoMb = 2 * 1024 * 1024L
        assertTrue(status.canPerformLargeTransfer(twoMb))

        // Large 30 MB file exceeds 15 MB cellular threshold to protect data caps
        val thirtyMb = 30 * 1024 * 1024L
        assertFalse(status.canPerformLargeTransfer(thirtyMb))
    }

    @Test
    fun `zero connection activates offline room fallback without crash`() {
        val status = NetworkCapabilityEvaluator.evaluate(
            hasInternet = false,
            isWifi = false,
            isCellular = false
        )

        assertEquals(ConnectionType.OFFLINE, status.connectionType)
        assertFalse(status.isConnected)
        assertTrue(status.offlineFallbackActive)

        assertEquals(SyncStrategy.LOCAL_ROOM_FALLBACK, status.getSyncStrategy())

        // Transfers rejected in offline mode
        assertFalse(status.canPerformLargeTransfer(1024L))
    }

    @Test
    fun `metered wifi hotspot behaves with data saver awareness`() {
        // A user tethered to a mobile hotspot over Wi-Fi
        val status = NetworkCapabilityEvaluator.evaluate(
            hasInternet = true,
            isWifi = true,
            isCellular = false,
            isNotMetered = false
        )

        assertEquals(ConnectionType.WIFI, status.connectionType)
        assertTrue(status.isConnected)
        assertTrue(status.isMetered)
        assertFalse(status.isHighBandwidth)
    }

    @Test
    fun `network manager status flow transitions reactively`() {
        val manager = NetworkConnectionManager(context = null)

        // Simulate initial Wi-Fi
        manager.setStatusForTesting(NetworkStatus.WIFI)
        assertEquals(ConnectionType.WIFI, manager.currentStatus.connectionType)
        assertEquals(SyncStrategy.IMMEDIATE_FULL_SYNC, manager.currentStatus.getSyncStrategy())

        // Transition to Cellular
        manager.setStatusForTesting(NetworkStatus.CELLULAR)
        assertEquals(ConnectionType.CELLULAR, manager.currentStatus.connectionType)
        assertEquals(SyncStrategy.METERED_DATA_SAVER, manager.currentStatus.getSyncStrategy())

        // Drop to Airplane mode / Offline
        manager.setStatusForTesting(NetworkStatus.OFFLINE)
        assertEquals(ConnectionType.OFFLINE, manager.currentStatus.connectionType)
        assertTrue(manager.currentStatus.offlineFallbackActive)
        assertEquals(SyncStrategy.LOCAL_ROOM_FALLBACK, manager.currentStatus.getSyncStrategy())
    }
}
