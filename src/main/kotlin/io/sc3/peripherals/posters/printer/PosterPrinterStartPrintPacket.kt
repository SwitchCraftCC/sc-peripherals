package io.sc3.peripherals.posters.printer

import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals.ModId
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos

data class PosterPrinterStartPrintPacket(
  val pos: BlockPos,
  val posterId: String,
) : ScLibraryPacket() {
  override val id = PosterPrinterStartPrintPacket.id

  companion object {
    val id = ModId("poster_printer_start_print")

    fun fromBytes(buf: PacketByteBuf) = PosterPrinterStartPrintPacket(
      pos = buf.readBlockPos(),
      posterId = buf.readString(),
    )
  }

  override fun toBytes(buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeString(posterId)
  }

  override fun onClientReceive(client: MinecraftClient, handler: ClientPlayNetworkHandler,
                               responseSender: PacketSender) {
    super.onClientReceive(client, handler, responseSender)

    val printer = client.world?.getBlockEntity(pos) as? PosterPrinterBlockEntity ?: return
    printer.animatingPosterId = posterId
    printer.animationStartTime = client.world?.time ?: 0
  }
}
