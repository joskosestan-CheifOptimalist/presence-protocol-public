package com.presenceprotocol.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class LedgerStats(val verifiedToday:Int, val pending:Int, val yieldToday:Double, val total:Double)

interface MiningLedger {
    fun observeStats(): Flow<LedgerStats>
}

class StubMiningLedger: MiningLedger {
    override fun observeStats(): Flow<LedgerStats> = flowOf(LedgerStats(0,0,0.0,0.0))
}
