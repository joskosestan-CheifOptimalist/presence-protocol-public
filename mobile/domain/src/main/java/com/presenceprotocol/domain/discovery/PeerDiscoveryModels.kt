package com.presenceprotocol.domain.discovery

import java.time.Instant

/** Represents a nearby peer sighting emitted by BLE discovery. */
data class PeerEvent(
    val peerId: String,
    val timestamp: Instant
)

/** Aggregated metrics for UI consumption. */
data class PeerDiscoveryMetrics(
    val peersNearby: Int = 0,
    val peersSeenLast10Minutes: Int = 0
)
