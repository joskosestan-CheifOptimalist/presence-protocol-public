package com.presenceprotocol.app.ui

import com.presenceprotocol.app.PresenceApp
import com.presenceprotocol.data.ble.PresenceDiscoveryController
import com.presenceprotocol.data.ble.gatt.PresenceGattServer
import com.presenceprotocol.domain.InMemoryMiningLedger
import com.presenceprotocol.domain.SyncCoordinator

object DashboardViewModelClient {
    fun default(): DashboardViewModel {
        val context = PresenceApp.appContext
        val ledger = InMemoryMiningLedger()
        val gattServer = PresenceGattServer(context)
        return DashboardViewModel(
            ledger = ledger,
            gattServer = gattServer,
            syncCoordinator = SyncCoordinator(),
            discoveryController = PresenceDiscoveryController(context, gattServer, ledger)
        )
    }
}
