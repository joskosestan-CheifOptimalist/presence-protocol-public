package com.presenceprotocol.app

enum class BleRole { CLIENT_ONLY, SERVER_ONLY, BOTH }

object BleConfig {
    const val BLE_ROLE = BleRole.CLIENT_ONLY
}
