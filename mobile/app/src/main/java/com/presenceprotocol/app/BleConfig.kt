package com.presenceprotocol.app

enum class BleRole { CLIENT_ONLY, SERVER_ONLY, BOTH }

object BleConfig {
    val BLE_ROLE: BleRole = BleRole.valueOf(BuildConfig.BLE_ROLE)
}
