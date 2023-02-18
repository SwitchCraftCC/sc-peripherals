package io.sc3.peripherals.posters

import io.prometheus.client.Counter
import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripheralsPrometheus.registry
import io.sc3.peripherals.ScPeripherals
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity

private const val MAX_POSTER_REQUESTS_PER_PACKET = 50

data class PosterRequestC2SPacket(
  val posterIds: List<String>
) : ScLibraryPacket() {
  override val id = PosterRequestC2SPacket.id

  companion object {
    val id = ScPeripherals.ModId("poster_request")

    private val requestCounter = Counter.build()
      .name("sc_peripherals_posters_requested")
      .help("Number of posters requested by clients")
      .register(registry)

    private val responseCounter = Counter.build()
      .name("sc_peripherals_posters_sent")
      .help("Number of posters sent by the server")
      .register(registry)

    fun fromBytes(buf: PacketByteBuf) = buf.run {
      PosterRequestC2SPacket(
        posterIds = readList { it.readString() }
      )
    }
  }

  override fun toBytes(buf: PacketByteBuf) {
    with(buf) {
      writeCollection(posterIds) { b, it -> b.writeString(it) }
    }
  }

  override fun onServerReceive(
    server: MinecraftServer,
    player: ServerPlayerEntity,
    handler: ServerPlayNetworkHandler,
    responseSender: PacketSender
  ) {
    server.submit {
      requestCounter.inc(posterIds.size.toDouble())

      for ((idx, posterId) in posterIds.withIndex()) {
        if (idx >= MAX_POSTER_REQUESTS_PER_PACKET) break

        PosterItem.getPosterState(posterId, server.overworld)?.let { state ->
          responseSender.sendPacket(state.toPacket(posterId))
          responseCounter.inc()
        }
      }
    }
  }
}
