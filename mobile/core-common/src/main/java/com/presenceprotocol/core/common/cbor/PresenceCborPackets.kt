package com.presenceprotocol.core.common.cbor

import com.presenceprotocol.core.common.handshake.HelloPacket
import com.presenceprotocol.core.common.handshake.ReplyPacket
import com.upokecenter.cbor.CBORObject

object PresenceCborPackets {
    fun decodeHello(bytes: ByteArray): HelloPacket {
        val map = CBORObject.DecodeFromBytes(bytes)
        return HelloPacket(
            version = map.getField(KEY_VERSION).AsInt32(),
            sessionId = map.getField(KEY_SESSION_ID).asByteString(),
            nonce = map.getField(KEY_NONCE).asByteString(),
            clientPublicKey = map.getField(KEY_CLIENT_PK).asByteString(),
            timestampSeconds = map.getField(KEY_TIMESTAMP).AsInt64()
        )
    }

    fun encodeHello(packet: HelloPacket): ByteArray {
        val obj = CBORObject.NewMap().apply {
            putKey(KEY_VERSION, CBORObject.FromObject(packet.version))
            putKey(KEY_SESSION_ID, CBORObject.FromObject(packet.sessionId))
            putKey(KEY_NONCE, CBORObject.FromObject(packet.nonce))
            putKey(KEY_CLIENT_PK, CBORObject.FromObject(packet.clientPublicKey))
            putKey(KEY_TIMESTAMP, CBORObject.FromObject(packet.timestampSeconds))
        }
        return obj.EncodeToBytes()
    }

    fun decodeReply(bytes: ByteArray): ReplyPacket {
        val map = CBORObject.DecodeFromBytes(bytes)
        return ReplyPacket(
            version = map.getField(KEY_VERSION).AsInt32(),
            sessionId = map.getField(KEY_SESSION_ID).asByteString(),
            nonce = map.getField(KEY_NONCE).asByteString(),
            serverPublicKey = map.getField(KEY_SERVER_PK).asByteString(),
            signature = map.getField(KEY_SIGNATURE).asByteString(),
            statusCode = map.getField(KEY_STATUS).AsInt32()
        )
    }

    fun encodeReply(packet: ReplyPacket): ByteArray {
        val obj = CBORObject.NewMap().apply {
            putKey(KEY_VERSION, CBORObject.FromObject(packet.version))
            putKey(KEY_SESSION_ID, CBORObject.FromObject(packet.sessionId))
            putKey(KEY_NONCE, CBORObject.FromObject(packet.nonce))
            putKey(KEY_SERVER_PK, CBORObject.FromObject(packet.serverPublicKey))
            putKey(KEY_SIGNATURE, CBORObject.FromObject(packet.signature))
            putKey(KEY_STATUS, CBORObject.FromObject(packet.statusCode))
        }
        return obj.EncodeToBytes()
    }

    private fun CBORObject.putKey(key: String, value: CBORObject) {
        this.Add(CBORObject.FromObject(key), value)
    }

    private fun CBORObject.getField(key: String): CBORObject =
        this[CBORObject.FromObject(key)]

    private fun CBORObject.asByteString(): ByteArray = this.GetByteString()

    private const val KEY_VERSION = "v"
    private const val KEY_SESSION_ID = "sid"
    private const val KEY_NONCE = "nonce"
    private const val KEY_CLIENT_PK = "pk_c"
    private const val KEY_SERVER_PK = "pk_s"
    private const val KEY_SIGNATURE = "sig"
    private const val KEY_STATUS = "code"
    private const val KEY_TIMESTAMP = "ts"
}
