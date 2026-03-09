package com.presenceprotocol.data.ble

data class EncounterTicket(
    val encounterId: String,
    val deviceAEphemeralKey: String,
    val deviceBEphemeralKey: String,
    val helloHash: String,
    val replyHash: String,
    val handshakeTimestamp: Long,
    val heartbeatId: Long,
    val epochId: Long,
    val heartbeatIndexInEpoch: Int,
    val nonce: String,
    val protocolVersion: String,
    val appVersion: String,
    val deviceASignature: String,
    val deviceBSignature: String
) {
    fun toJson(): String {
        return """{"encounterId":"$encounterId","deviceAEphemeralKey":"$deviceAEphemeralKey","deviceBEphemeralKey":"$deviceBEphemeralKey","helloHash":"$helloHash","replyHash":"$replyHash","handshakeTimestamp":$handshakeTimestamp,"heartbeatId":$heartbeatId,"epochId":$epochId,"heartbeatIndexInEpoch":$heartbeatIndexInEpoch,"nonce":"$nonce","protocolVersion":"$protocolVersion","appVersion":"$appVersion","deviceASignature":"$deviceASignature","deviceBSignature":"$deviceBSignature"}"""
    }
}
