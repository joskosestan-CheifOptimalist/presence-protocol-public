package com.presenceprotocol.core.common.cbor

/**
 * Packet models live in :core-common so CBOR encode/decode can compile without depending on :domain.
 * These match the fields used by PresenceCborPackets.kt.
 */
data class HelloPacket(
    val version: Int,
    val sessionId: ByteArray,
    val nonce: ByteArray,
    val clientPublicKey: ByteArray,
    val timestampSeconds: Long
)

data class ReplyPacket(
    val version: Int,
    val sessionId: ByteArray,
    val nonce: ByteArray,
    val serverPublicKey: ByteArray,
    val signature: ByteArray,
    val statusCode: Int
)
