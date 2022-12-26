package io.sc3.peripherals.prints.printer

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos
import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.prints.PrintData

data class PrinterDataPacket(
  val pos: BlockPos,
  val data: PrintData?
) : ScLibraryPacket() {
  override val id = PrinterDataPacket.id

  companion object {
    val id = ModId("printer_data")

    fun fromBytes(buf: PacketByteBuf) = PrinterDataPacket(
      pos = buf.readBlockPos(),
      data = buf.readNullable(PacketByteBuf::readNbt)?.let { PrintData.fromNbt(it) }
    )
  }

  override fun toBytes(buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeNullable(data?.toNbt(), PacketByteBuf::writeNbt)
  }

  override fun onClientReceive(client: MinecraftClient, handler: ClientPlayNetworkHandler,
                               responseSender: PacketSender) {
    super.onClientReceive(client, handler, responseSender)

    val printer = client.world?.getBlockEntity(pos) as? PrinterBlockEntity ?: return
    if (data != null) printer.previewData = data
  }
}
