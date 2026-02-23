package com.presenceprotocol.domain.encounter

import java.time.Duration
import java.time.Instant

interface EncounterCooldownPolicy {
    val window: Duration
    fun isCoolingDown(peers: EncounterPeers, now: Instant): Boolean
    fun recordReward(peers: EncounterPeers, rewardedAt: Instant)
}

class InMemoryEncounterCooldownPolicy(
    override val window: Duration
) : EncounterCooldownPolicy {
    private val lastRewarded = mutableMapOf<String, Instant>()

    override fun isCoolingDown(peers: EncounterPeers, now: Instant): Boolean {
        val key = peers.canonicalPairKey()
        val last = lastRewarded[key] ?: return false
        return Duration.between(last, now) < window
    }

    override fun recordReward(peers: EncounterPeers, rewardedAt: Instant) {
        lastRewarded[peers.canonicalPairKey()] = rewardedAt
    }
}
