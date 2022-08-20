package pw.switchcraft.peripherals.prints.printer

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos
import pw.switchcraft.peripherals.prints.PrintData
import pw.switchcraft.peripherals.util.ScPeripheralsPacket
import pw.switchcraft.peripherals.util.packetId

data class PrinterDataPacket(
  val pos: BlockPos,
  val data: PrintData?
) : ScPeripheralsPacket() {
  override val id = PrinterDataPacket.id

  companion object {
    val id = packetId("printer_data")

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
