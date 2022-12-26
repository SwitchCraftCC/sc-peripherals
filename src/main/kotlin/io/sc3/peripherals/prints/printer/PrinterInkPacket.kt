package io.sc3.peripherals.prints.printer

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos
import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals.ModId

data class PrinterInkPacket(
  val pos: BlockPos,
  val chamelium: Int,
  val ink: Int
) : ScLibraryPacket() {
  override val id = PrinterInkPacket.id

  companion object {
    val id = ModId("printer_ink")

    fun fromBytes(buf: PacketByteBuf) = PrinterInkPacket(
      pos = buf.readBlockPos(),
      chamelium = buf.readInt(),
      ink = buf.readInt()
    )
  }

  override fun toBytes(buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeInt(chamelium)
    buf.writeInt(ink)
  }

  override fun onClientReceive(client: MinecraftClient, handler: ClientPlayNetworkHandler,
                               responseSender: PacketSender) {
    super.onClientReceive(client, handler, responseSender)

    val printer = client.world?.getBlockEntity(pos) as? PrinterBlockEntity ?: return
    printer.chamelium = chamelium
    printer.ink = ink
  }
}
