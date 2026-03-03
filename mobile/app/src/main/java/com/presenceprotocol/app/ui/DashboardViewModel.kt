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
            discoveryController.start()
            discoveryStarted = true
        }
    }

    fun stopDiscovery() {
        if (discoveryStarted) {
            discoveryController.stop()
            discoveryStarted = false
       }
    }


    fun toggleMining() {
        android.util.Log.e("PP_BLE", "VM: toggle handler entered")
        val next = !_uiState.value.isMining
        _uiState.value = _uiState.value.copy(isMining = next)

        android.util.Log.e("PP_BLE", "VM: BLE_ROLE=${BleConfig.BLE_ROLE} next=$next")

        if (next) {
            // Updated conditional logic for BOTH role
            if (BleConfig.BLE_ROLE == BleRole.CLIENT_ONLY) {
                // Only start discovery for client
                android.util.Log.e("PP_BLE", "VM: starting DISCOVERY")
                discoveryController.start()
            } else if (BleConfig.BLE_ROLE == BleRole.SERVER_ONLY) {
                // Start GATT server + advertise for server
                android.util.Log.e("PP_BLE", "VM: starting GATT SERVER")
                gattServer.start()
                android.util.Log.e("PP_BLE", "VM: starting ADVERTISING")
                gattServer.startAdvertising(connectable = true)
            } else if (BleConfig.BLE_ROLE == BleRole.BOTH) {
                // Start both discovery + advertising + GATT server
                android.util.Log.e("PP_BLE", "VM: starting DISCOVERY + ADVERTISING + GATT SERVER")
                discoveryController.start()
                gattServer.start()
                gattServer.startAdvertising(connectable = true)
            }
        } else {
            android.util.Log.e("PP_BLE", "VM: disabling server + discovery")
            discoveryController.stop()
            if (BleConfig.BLE_ROLE != BleRole.CLIENT_ONLY) {
                gattServer.stop()
            } else {
                android.util.Log.e("PP_BLE", "VM: skipping GATT SERVER stop (client-only)")
            }
        }
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
    val devLog: List<String> = emptyList()
)