package com.presenceprotocol.core.common.config

enum class TransportMode { RAW_PROBE, CBOR_PROBE }

object TransportConfig {
    const val CURRENT_TRANSPORT_MODE: String = "RAW_PROBE"
    val transportMode: TransportMode = TransportMode.valueOf(CURRENT_TRANSPORT_MODE)
}
