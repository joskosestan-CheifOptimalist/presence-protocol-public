package com.presenceprotocol.app.ui

import com.presenceprotocol.data.ble.gatt.PresenceGattServer
import com.presenceprotocol.app.BleConfig
import com.presenceprotocol.app.BleRole

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.presenceprotocol.data.ble.PresenceDiscoveryController
import com.presenceprotocol.domain.MiningLedger
import com.presenceprotocol.domain.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val ledger: MiningLedger,
    private val gattServer: PresenceGattServer,
    private val syncCoordinator: SyncCoordinator,
    private val discoveryController: PresenceDiscoveryController
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private var discoveryStarted = false
    private var heartbeatJob: Job? = null

    init {
        viewModelScope.launch {
            ledger.observeStats().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    verifiedToday = stats.verifiedToday,
                    pendingEncounters = stats.pending,
                    todayYield = stats.yieldToday,
                    totalBalance = stats.total
                )
            }
        }
        viewModelScope.launch {
            discoveryController.metrics.collect { metrics ->
                _uiState.value = _uiState.value.copy(
                    peersNearby = metrics.peersNearby,
                    peersSeenLast10Minutes = metrics.peersSeenLast10Minutes,
                    statusText = if (metrics.peersNearby > 0) "Peers nearby" else "Scanning..."
                )
            }
        }
        viewModelScope.launch {
            discoveryController.peerEvents.collect { event ->
                appendLog("Peer ${event.peerId} seen @ ${event.timestamp}")
            }
        }
    }

    fun ensureDiscoveryStarted() {
        if (!discoveryStarted) {
            gattServer.start(); // Start the server first
            discoveryController.start()
            discoveryStarted = true
            startHeartbeat()
            _uiState.value = _uiState.value.copy(isMining = true)
        }
    }

    fun stopDiscovery() {
        android.util.Log.e("PP_BLE", "VM: stopDiscovery entered discoveryStarted=" + discoveryStarted)
        if (discoveryStarted) {
            android.util.Log.e("PP_BLE", "VM: stopDiscovery -> stopping discovery + gatt server")
            discoveryController.stop()
            gattServer.stop()
            discoveryStarted = false
            stopHeartbeat()
            _uiState.value = _uiState.value.copy(isMining = false)
        }
    }

    fun toggleMining() {
        android.util.Log.e("PP_BLE", "VM: toggle handler entered")
        val next = !_uiState.value.isMining
        _uiState.value = _uiState.value.copy(isMining = next)

        android.util.Log.e("PP_BLE", "VM: BLE_ROLE=${BleConfig.BLE_ROLE} next=$next")

        if (next) {
            if (BleConfig.BLE_ROLE == BleRole.CLIENT_ONLY) {
                android.util.Log.e("PP_BLE", "VM: starting DISCOVERY")
                discoveryController.start()
            } else if (BleConfig.BLE_ROLE == BleRole.SERVER_ONLY) {
                android.util.Log.e("PP_BLE", "VM: starting GATT SERVER + DISCOVERY")
                gattServer.start()
                discoveryController.start()
            } else if (BleConfig.BLE_ROLE == BleRole.BOTH) {
                android.util.Log.e("PP_BLE", "VM: starting DISCOVERY + GATT SERVER")
                gattServer.start(); // Ensure server starts first
                discoveryController.start()
            }
        } else {
            android.util.Log.e("PP_BLE", "VM: toggle OFF -> disabling server + discovery")
            discoveryController.stop()
            if (BleConfig.BLE_ROLE != BleRole.CLIENT_ONLY) {
                gattServer.stop()
            } else {
                android.util.Log.e("PP_BLE", "VM: skipping GATT SERVER stop (client-only)")
            }
        }
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = viewModelScope.launch {
            while (true) {
                val nextEpoch = _uiState.value.epoch + 1
                _uiState.value = _uiState.value.copy(
                    heartbeatTick = _uiState.value.heartbeatTick + 1,
                    lastHeartbeatAt = System.currentTimeMillis(),
                    epoch = nextEpoch,
                    networkHealth = "Live"
                )
                delay(1200)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun showDeveloperPanel(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeveloperPanel = show)
    }

    fun appendLog(event: String) {
        val updated = buildList {
            add(event)
            addAll(_uiState.value.devLog.take(49))
        }
        _uiState.value = _uiState.value.copy(devLog = updated)
    }
}

data class DashboardUiState(
    val peersNearby: Int = 0,
    val peersSeenLast10Minutes: Int = 0,
    val statusText: String = "Idle • Waiting for signal",
    val isMining: Boolean = false,
    val verifiedToday: Int = 0,
    val pendingEncounters: Int = 0,
    val todayYield: Double = 0.0,
    val totalBalance: Double = 0.0,
    val networkHealth: String = "Stable",
    val showDeveloperPanel: Boolean = false,
    val devLog: List<String> = emptyList(),
    val heartbeatTick: Long = 0L,
    val lastHeartbeatAt: Long = 0L,
    val epoch: Int = 0
)
