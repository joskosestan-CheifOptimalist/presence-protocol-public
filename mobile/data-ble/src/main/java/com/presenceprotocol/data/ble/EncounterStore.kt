package com.presenceprotocol.data.ble

interface EncounterStore {
    fun append(ticket: EncounterTicket): Boolean
    fun count(): Int
}
