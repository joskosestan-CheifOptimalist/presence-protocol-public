package com.presenceprotocol.app.ui

import com.presenceprotocol.app.PresenceApp
import com.presenceprotocol.data.ble.PresenceDiscoveryController
import com.presenceprotocol.data.ble.gatt.PresenceGattServer
import com.presenceprotocol.domain.StubMiningLedger
import com.presenceprotocol.domain.SyncCoordinator

object DashboardViewModelClient {
    fun default(): DashboardViewModel {
        val context = PresenceApp.appContext
        return DashboardViewModel(
            ledger = StubMiningLedger(),
            gattServer = PresenceGattServer(context),
            syncCoordinator = SyncCoordinator(),
            discoveryController = PresenceDiscoveryController(context)
        )
    }
}
