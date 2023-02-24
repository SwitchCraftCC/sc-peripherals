package io.sc3.peripherals.posters

import io.sc3.peripherals.config.ScPeripheralsClientConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World
import kotlin.time.Duration.Companion.seconds

private val posterRequestQueue: MutableList<String> = mutableListOf()
private val requestedPosters: MutableMap<String, Instant> = mutableMapOf()
private val requestTimeout = 10.seconds

fun tickPosterRequests(world: ClientWorld) {
  val batch = posterRequestQueue.take(ScPeripheralsClientConfig.config["maxPosterRequestsPerTick"])
  if (batch.isNotEmpty()) {
    MinecraftClient.getInstance().player?.networkHandler?.sendPacket(PosterRequestC2SPacket(batch).toC2SPacket())

    // Remove the posters we just requested from the queue
    posterRequestQueue.removeAll(batch)
  }
}

private val mapStates: MutableMap<String, PosterState> = mutableMapOf()
fun World.getPosterState(name: String): PosterState? {
  return if (isClient) {
    mapStates[name].also {
      if (it == null) {
        val posterId = PosterItem.getIdFromName(name)
        val request = requestedPosters[posterId]
        if (request == null || Clock.System.now() - request > requestTimeout) {
          posterRequestQueue.add(posterId)
          requestedPosters[posterId] = Clock.System.now()
        }
      }
    }
  } else {
    server!!.overworld
      .persistentStateManager.get({ nbt: NbtCompound? ->
        nbt?.let { PosterState.fromNbt(it) }
      }, name)
  }
}

fun World.putPosterState(name: String, state: PosterState) {
  if (isClient) {
    mapStates[name] = state
  } else {
    server!!.overworld.persistentStateManager.set(name, state)
  }
}
