package pw.switchcraft.peripherals.util

import net.minecraft.server.world.ServerChunkManager
import net.minecraft.world.chunk.WorldChunk

object NetworkUtil {
  fun sendToAllTracking(chunk: WorldChunk, packet: ScPeripheralsPacket) {
    val storage = (chunk.world.chunkManager as ServerChunkManager).threadedAnvilChunkStorage
    storage.getPlayersWatchingChunk(chunk.pos, false).forEach {
      it.networkHandler.sendPacket(packet.toS2CPacket())
    }
  }
}
