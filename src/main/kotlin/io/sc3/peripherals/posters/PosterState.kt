package io.sc3.peripherals.posters

import io.sc3.library.networking.ScLibraryPacket
import io.sc3.peripherals.Registration.ModItems.poster
import io.sc3.peripherals.posters.PosterItem.Companion.getPosterId
import io.sc3.peripherals.posters.PosterItem.Companion.getPosterState
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import java.io.File

class PosterState : PersistentState() {
  var colors = ByteArray(16384)
  var palette = getDefaultPalette() // Default to map colors

  override fun save(file: File) {
    file.parentFile.mkdirs()
    super.save(file)
  }

  private val updateTrackersByPlayer: MutableMap<PlayerEntity, PlayerUpdateTracker> = mutableMapOf()

  fun setColor(x: Int, z: Int, color: Byte) {
    colors[x + z * 128] = color
    markDirty(x, z)
  }

  private fun markDirty(x: Int, z: Int) {
    this.markDirty()
    for (playerUpdateTracker in updateTrackersByPlayer.values) {
      playerUpdateTracker.markDirty(x, z)
    }
  }

  fun getPlayerUpdatePacket(id: String, player: PlayerEntity?): ScLibraryPacket? {
    return updateTrackersByPlayer[player]?.getPacket(id)
  }

  fun update(player: PlayerEntity) {
    synchronized(updateTrackersByPlayer) {
      if (!this.updateTrackersByPlayer.containsKey(player)) {
        val playerUpdateTracker: PosterState.PlayerUpdateTracker = this.PlayerUpdateTracker(player)
        this.updateTrackersByPlayer[player] = playerUpdateTracker
      }
    }
  }

  fun pruneTrackers(stack: ItemStack) {
    synchronized(updateTrackersByPlayer) {
      for (tracker in updateTrackersByPlayer.values) {
        if (tracker.player.isRemoved || !(tracker.player.inventory.contains(stack) || stack.isInFrame)) {
          this.updateTrackersByPlayer.remove(tracker.player)
        }
      }
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(PosterState::class.java)

    fun fromNbt(nbt: NbtCompound): PosterState? {
      val posterState = PosterState()
      val colorArray = nbt.getByteArray("colors")
      if (colorArray.size == 16384) {
        posterState.colors = colorArray
      }
      val paletteArray = nbt.getIntArray("palette")
      if (paletteArray.size <= 64) {
        posterState.palette = paletteArray
      }

      return posterState
    }

    @JvmStatic
    fun tickEntityTracker(entity: ItemFrameEntity, world: ServerWorld) {
      try {
        val itemStack = entity.heldItemStack

        if (itemStack.item === poster) {
          val id = getPosterId(itemStack) ?: return
          val posterState = getPosterState(id, world)
          if (posterState != null) {

            // TODO: Don't send to players who can't see the poster
            for (serverPlayerEntity in world.players) {
              posterState.update(serverPlayerEntity)
              val packet = posterState.getPlayerUpdatePacket(id, serverPlayerEntity)
              if (packet != null) {
                serverPlayerEntity.networkHandler.sendPacket(packet.toS2CPacket())
              }
            }
            posterState.pruneTrackers(itemStack)
          }
        }
      } catch (e: Exception) {
        logger.error("Error ticking entity tracker", e)
      }
    }
  }

  override fun writeNbt(nbt: NbtCompound) = nbt.apply {
    putByteArray("colors", colors)
    putIntArray("palette", palette)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PosterState

    if (!colors.contentEquals(other.colors)) return false
    if (!palette.contentEquals(other.palette)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = colors.contentHashCode()
    result = 31 * result + palette.contentHashCode()
    return result
  }

  inner class PlayerUpdateTracker internal constructor(val player: PlayerEntity) {
    private var dirty = true
    private var startX = 0
    private var startZ = 0
    private var endX = 127
    private var endZ = 127

    private val mapUpdateData: UpdateData
      private get() {
        val i = startX
        val j = startZ
        val k = endX + 1 - startX
        val l = endZ + 1 - startZ
        val bs = ByteArray(k * l)
        for (m in 0 until k) {
          for (n in 0 until l) {
            bs[m + n * k] = this@PosterState.colors[i + m + (j + n) * 128]
          }
        }
        val pal = this@PosterState.palette.clone()
        return UpdateData(i, j, k, l, bs, pal)
      }

    fun getPacket(posterId: String): ScLibraryPacket? {
      val updateData: UpdateData?
      if (dirty) {
        dirty = false
        updateData = mapUpdateData
      } else {
        updateData = null
      }

      return if (updateData == null) null else PosterUpdateS2CPacket(
        posterId,
        updateData
      )
    }

    fun markDirty(startX: Int, startZ: Int) {
      if (dirty) {
        this.startX = Math.min(this.startX, startX)
        this.startZ = Math.min(this.startZ, startZ)
        endX = Math.max(endX, startX)
        endZ = Math.max(endZ, startZ)
      } else {
        dirty = true
        this.startX = startX
        this.startZ = startZ
        endX = startX
        endZ = startZ
      }
    }
  }

  class UpdateData(
    val startX: Int,
    val startZ: Int,
    val width: Int,
    val height: Int,
    val colors: ByteArray,
    val palette: IntArray
  ) {
    fun setColorsTo(posterState: PosterState) {
      for (i in 0 until width) {
        for (j in 0 until height) {
          posterState.setColor(startX + i, startZ + j, colors[i + j * width])
        }
      }
    }

    fun setPaletteTo(posterState: PosterState) {
      posterState.palette = palette
    }
  }
}
