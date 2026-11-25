package com.splitsignal.core.network

import com.splitsignal.core.domain.DiscoveredRoom
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.LanDiscovery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Browser-safe no-op [LanDiscovery] for Wasm targets where raw UDP sockets are unavailable.
 */
class WasmLanDiscovery : LanDiscovery {
    override fun startBroadcasting(roomCode: String, wsPort: Int, gameType: GameType) = Unit

    override fun stopBroadcasting() = Unit

    override fun observeNearbyRooms(): Flow<List<DiscoveredRoom>> = flowOf(emptyList())
}
