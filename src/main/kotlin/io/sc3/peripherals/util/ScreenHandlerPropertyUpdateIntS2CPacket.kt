package io.sc3.peripherals.util

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.createS2CPacket
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.ScreenHandlerPropertyUpdateS2CPacket
import net.minecraft.server.network.ServerPlayNetworkHandler
import io.sc3.peripherals.ScPeripherals.ModId

class ScreenHandlerPropertyUpdateIntS2CPacket(
  syncId: Int,
  propertyId: Int,
  value: Int
) : ScreenHandlerPropertyUpdateS2CPacket(syncId, propertyId, value) {
  constructor (buf: PacketByteBuf) : this(
    buf.readUnsignedByte().toInt(),
    buf.readInt(),
    buf.readInt()
  )

  override fun write(buf: PacketByteBuf) {
    buf.writeByte(syncId)
    buf.writeInt(propertyId)
    buf.writeInt(value)
  }

  override fun apply(listener: ClientPlayPacketListener) {
    listener.onScreenHandlerPropertyUpdate(this)
  }

  fun send(handler: ServerPlayNetworkHandler) {
    val buf = PacketByteBufs.create()
    write(buf)
    handler.sendPacket(createS2CPacket(id, buf))
  }

  companion object {
    val id = ModId("screen-handler-property-update-int")

    fun registerReceiver() {
      ClientPlayNetworking.registerGlobalReceiver(id) { client, handler, buf, _ ->
        val packet = ScreenHandlerPropertyUpdateIntS2CPacket(buf)
        client.submit { packet.apply(handler) }
      }
    }
  }
}
