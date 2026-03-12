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
    fun isValid(): Boolean {
        return encounterId.isNotBlank() &&
            deviceAEphemeralKey.isNotBlank() &&
            deviceBEphemeralKey.isNotBlank() &&
            helloHash.isNotBlank() &&
            replyHash.isNotBlank() &&
            handshakeTimestamp > 0L &&
            nonce.isNotBlank() &&
            protocolVersion.isNotBlank() &&
            appVersion.isNotBlank() &&
            deviceASignature.isNotBlank() &&
            deviceBSignature.isNotBlank()
    }

    fun toJson(): String {
        return """{"encounterId":"${escapeJson(encounterId)}","deviceAEphemeralKey":"${escapeJson(deviceAEphemeralKey)}","deviceBEphemeralKey":"${escapeJson(deviceBEphemeralKey)}","helloHash":"${escapeJson(helloHash)}","replyHash":"${escapeJson(replyHash)}","handshakeTimestamp":$handshakeTimestamp,"heartbeatId":$heartbeatId,"epochId":$epochId,"heartbeatIndexInEpoch":$heartbeatIndexInEpoch,"nonce":"${escapeJson(nonce)}","protocolVersion":"${escapeJson(protocolVersion)}","appVersion":"${escapeJson(appVersion)}","deviceASignature":"${escapeJson(deviceASignature)}","deviceBSignature":"${escapeJson(deviceBSignature)}"}"""
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
