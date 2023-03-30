package io.sc3.peripherals.posters

import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals
import io.sc3.peripherals.client.item.PosterRenderer
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf

data class PosterUpdateS2CPacket(
  val posterId: String,
  private val updateData: PosterState.UpdateData?
) : ScLibraryPacket() {
  override val id = PosterUpdateS2CPacket.id

  companion object {
    val id = ScPeripherals.ModId("poster_update")

    fun fromBytes(buf: PacketByteBuf) = buf.run {
      PosterUpdateS2CPacket(
        posterId = readString(),
        updateData = PosterState.UpdateData(
          width = readUnsignedByte().toInt(),
          height = readUnsignedByte().toInt(),
          startX = readUnsignedByte().toInt(),
          startZ = readUnsignedByte().toInt(),
          colors = readByteArray(),
          palette = readIntArray()
        )
      )
    }
  }

  override fun toBytes(buf: PacketByteBuf) {
    with(buf) {
      writeString(posterId)
      if (updateData != null) {
        writeByte(updateData.width)
        writeByte(updateData.height)
        writeByte(updateData.startX)
        writeByte(updateData.startZ)
        writeByteArray(updateData.colors)
        writeIntArray(updateData.palette)
      } else {
        writeByte(0)
      }
    }
  }

  private fun apply(posterState: PosterState) {
    if (updateData != null) {
      updateData.setColorsTo(posterState)
      updateData.setPaletteTo(posterState)
    }
  }

  override fun onClientReceive(
    client: MinecraftClient,
    handler: ClientPlayNetworkHandler,
    responseSender: PacketSender
  ) {
    client.submit {
      val name = PosterItem.getPosterName(posterId)
      var posterState: PosterState? = client.world?.getPosterState(name)
      if (posterState == null) {
        posterState = PosterState()
        client.world?.putPosterState(name, posterState)
      }

      apply(posterState)
      PosterRenderer.updateTexture(posterId, posterState)
    }
  }
}
