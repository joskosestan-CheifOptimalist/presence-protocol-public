package com.presenceprotocol.core.crypto

object EphemeralKeys {
    fun generateNonce(): ByteArray = ByteArray(12) { (0..255).random().toByte() }
}
