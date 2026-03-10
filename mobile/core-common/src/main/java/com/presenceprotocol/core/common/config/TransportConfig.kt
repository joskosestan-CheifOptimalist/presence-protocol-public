package com.presenceprotocol.core.common.config

enum class TransportMode { CBOR }

object TransportConfig {
    const val CURRENT_TRANSPORT_MODE: String = "CBOR"
    const val LEDGER_CREDIT_COOLDOWN_MS: Long = 300_000L
    val transportMode: TransportMode = TransportMode.valueOf(CURRENT_TRANSPORT_MODE)
}
