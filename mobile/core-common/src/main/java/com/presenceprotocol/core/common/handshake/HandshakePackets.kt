package com.presenceprotocol.core.common.handshake

data class HelloPacket(
    val version: Int,
    val sessionId: ByteArray,
    val nonce: ByteArray,
    val clientPublicKey: ByteArray,
    val timestampSeconds: Long,
    val appInstanceId: String
)

data class ReplyPacket(
    val version: Int,
    val sessionId: ByteArray,
    val nonce: ByteArray,
    val serverPublicKey: ByteArray,
    val signature: ByteArray,
    val statusCode: Int,
    val appId: String,
    val appInstanceId: String
)
