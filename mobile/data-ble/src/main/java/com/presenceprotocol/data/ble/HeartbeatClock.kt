package com.presenceprotocol.data.ble

object HeartbeatClock {
    const val HEARTBEAT_MS = 15_000L
    const val HEARTBEATS_PER_EPOCH = 12L

    fun heartbeatId(nowMs: Long = System.currentTimeMillis()): Long {
        return nowMs / HEARTBEAT_MS
    }

    fun epochId(nowMs: Long = System.currentTimeMillis()): Long {
        return heartbeatId(nowMs) / HEARTBEATS_PER_EPOCH
    }

    fun heartbeatIndexInEpoch(nowMs: Long = System.currentTimeMillis()): Int {
        return (heartbeatId(nowMs) % HEARTBEATS_PER_EPOCH).toInt()
    }
}
