package io.sc3.peripherals.posters.printer

import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals.ModId
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos

data class PosterPrinterInkPacket(
  val pos: BlockPos,
  val ink: Int
) : ScLibraryPacket() {
  override val id = PosterPrinterInkPacket.id

  companion object {
    val id = ModId("poster_printer_ink")

    fun fromBytes(buf: PacketByteBuf) = PosterPrinterInkPacket(
      pos = buf.readBlockPos(),
      ink = buf.readInt()
    )
  }

  override fun toBytes(buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeInt(ink)
  }

  override fun onClientReceive(client: MinecraftClient, handler: ClientPlayNetworkHandler,
                               responseSender: PacketSender) {
    super.onClientReceive(client, handler, responseSender)

    val printer = client.world?.getBlockEntity(pos) as? PosterPrinterBlockEntity ?: return
    printer.ink = ink
  }
}
