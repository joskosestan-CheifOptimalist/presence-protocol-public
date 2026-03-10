package com.presenceprotocol.core.common.config

enum class TransportMode { CBOR }

object TransportConfig {
    const val CURRENT_TRANSPORT_MODE: String = "CBOR"
    val transportMode: TransportMode = TransportMode.valueOf(CURRENT_TRANSPORT_MODE)
}
