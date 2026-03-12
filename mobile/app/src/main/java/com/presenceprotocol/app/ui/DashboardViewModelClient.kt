package com.presenceprotocol.app.ui

import com.presenceprotocol.app.BleConfig
import com.presenceprotocol.app.BleRole
import com.presenceprotocol.app.PresenceApp
import com.presenceprotocol.data.ble.FileEncounterStore
import com.presenceprotocol.data.ble.PresenceHandshakeCoordinator
import com.presenceprotocol.data.ble.PresenceDiscoveryController
import com.presenceprotocol.data.ble.gatt.PresenceGattServer
import com.presenceprotocol.domain.InMemoryMiningLedger
import com.presenceprotocol.domain.SyncCoordinator

object DashboardViewModelClient {
    fun default(): DashboardViewModel {
        val context = PresenceApp.appContext
        val ledger = InMemoryMiningLedger()
        val encounterStore = FileEncounterStore(context)
        val handshakeCoordinator = PresenceHandshakeCoordinator(null, ledger, encounterStore)
        val gattServer = PresenceGattServer(context, handshakeCoordinator)
        android.util.Log.d("EncounterStore", "ENCOUNTER_STORE_STARTUP_COUNT count=${encounterStore.count()}")
        return DashboardViewModel(
            ledger = ledger,
            gattServer = gattServer,
            syncCoordinator = SyncCoordinator(),
            discoveryController = PresenceDiscoveryController(
                context,
                gattServer,
                ledger,
                encounterStore,
                allowInitiation = (BleConfig.BLE_ROLE == BleRole.CLIENT_ONLY || BleConfig.BLE_ROLE == BleRole.BOTH),
                providedHandshakeCoordinator = handshakeCoordinator
            )
        )
    }
}
