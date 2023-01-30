package io.sc3.peripherals.posters

import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

private val mapStates: MutableMap<String, PosterState> = mutableMapOf()
fun World.getPosterState(name: String): PosterState? {
  return if (isClient) {
    mapStates[name]
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
