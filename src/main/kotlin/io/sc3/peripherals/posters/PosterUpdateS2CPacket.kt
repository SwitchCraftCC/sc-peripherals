package io.sc3.peripherals.posters

import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.ScPeripherals
import io.sc3.peripherals.client.item.PosterRenderer
import io.sc3.peripherals.posters.PosterState.Companion.logger
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class PosterUpdateS2CPacket(
  val posterId: String,
  private val updateData: PosterState.UpdateData?
) : ScLibraryPacket() {
  override val id = PosterUpdateS2CPacket.id

  companion object {
    val logger: Logger = LoggerFactory.getLogger(PosterUpdateS2CPacket::class.java)

    val id = ScPeripherals.ModId("poster_update")

    fun fromBytes(buf: PacketByteBuf) = PosterUpdateS2CPacket(
      posterId = buf.readString(),
      updateData = PosterState.UpdateData(
        width=buf.readUnsignedByte().toInt(),
        height=buf.readUnsignedByte().toInt(),
        startX=buf.readUnsignedByte().toInt(),
        startZ=buf.readUnsignedByte().toInt(),
        colors=buf.readByteArray(),
        palette=buf.readIntArray()
      )
    )
  }

  override fun toBytes(buf: PacketByteBuf) {
    buf.writeString(posterId)
    if (updateData != null) {
      buf.writeByte(updateData.width)
      buf.writeByte(updateData.height)
      buf.writeByte(updateData.startX)
      buf.writeByte(updateData.startZ)
      buf.writeByteArray(updateData.colors)
      buf.writeIntArray(updateData.palette)
    } else {
      buf.writeByte(0)
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
      logger.info("Received poster update for $name: ${updateData?.colors}")
      if (posterState == null) {
        logger.info("Creating new poster state for $name")
        posterState = PosterState()
        client.world?.putPosterState(name, posterState)
      }

      apply(posterState)
      PosterRenderer.updateTexture(posterId, posterState)
    }
  }
}
