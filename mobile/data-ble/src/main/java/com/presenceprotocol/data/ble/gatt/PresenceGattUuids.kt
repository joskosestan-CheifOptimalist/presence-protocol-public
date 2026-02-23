package com.presenceprotocol.data.ble.gatt

import java.util.UUID

/**
 * Presence Protocol BLE GATT UUIDs
 *
 * Service UUID is locked to the discovery UUID already used in PresenceDiscoveryController.
 * Characteristic UUIDs are derived by incrementing the last byte:
 *  - ...c01 = service
 *  - ...c02 = HELLO (client -> server write)
 *  - ...c03 = REPLY (server -> client notify)
 *  - ...c04 = RESULT (client -> server write)
 */
object PresenceGattUuids {
    val PRESENCE_SERVICE_UUID: UUID =
        UUID.fromString("7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01")

    val HELLO_CHAR_UUID: UUID =
        UUID.fromString("7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c02")

    val REPLY_CHAR_UUID: UUID =
        UUID.fromString("7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c03")

    val RESULT_CHAR_UUID: UUID =
        UUID.fromString("7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c04")
}
