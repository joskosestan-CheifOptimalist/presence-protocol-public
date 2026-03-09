package com.presenceprotocol.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LedgerStats(val verifiedToday:Int, val pending:Int, val yieldToday:Double, val total:Double)

interface MiningLedger {
    fun observeStats(): Flow<LedgerStats>
    fun recordEncounter(yieldIncrement: Double = 0.01)
}

class InMemoryMiningLedger : MiningLedger {
    private val stats = MutableStateFlow(LedgerStats(0, 0, 0.0, 0.0))

    override fun observeStats(): Flow<LedgerStats> = stats.asStateFlow()

    override fun recordEncounter(yieldIncrement: Double) {
        val current = stats.value
        stats.value = current.copy(
            verifiedToday = current.verifiedToday + 1,
            pending = 0,
            yieldToday = current.yieldToday + yieldIncrement,
            total = current.total + yieldIncrement
        )
    }
}

class StubMiningLedger: MiningLedger {
    override fun observeStats(): Flow<LedgerStats> = MutableStateFlow(LedgerStats(0,0,0.0,0.0)).asStateFlow()
    override fun recordEncounter(yieldIncrement: Double) = Unit
}
