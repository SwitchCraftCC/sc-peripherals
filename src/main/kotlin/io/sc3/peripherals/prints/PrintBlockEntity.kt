package io.sc3.peripherals.prints

import io.sc3.library.ext.optCompound
import io.sc3.peripherals.Registration.ModBlockEntities.print
import io.sc3.peripherals.util.BaseBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class PrintBlockEntity(
  pos: BlockPos,
  state: BlockState
) : BaseBlockEntity(print, pos, state) {
  private var dataDirty = false
  var data: PrintData = PrintData()
    set(value) {
      field = value
      dataDirty = true
    }

  val on: Boolean
    get() = with(cachedState) { if (isAir) false else get(PrintBlock.on) }
  val facing: Direction
    get() = with(cachedState) { if (isAir) Direction.NORTH else get(PrintBlock.facing) }

  val canTurnOn
    get() = data.shapesOn.isNotEmpty()
  val shapes
    get() = if (on) data.shapesOn else data.shapesOff

  val collide
    get() = if (on) data.collideWhenOn else data.collideWhenOff

  private val emitsRedstone
    get() = data.redstoneLevel > 0
  private val emitsRedstoneWhenOff
    get() = emitsRedstone && !canTurnOn
  private val emitsRedstoneWhenOn
    get() = emitsRedstone && canTurnOn

  fun toggle() {
    val world = world ?: return
    val block = cachedState.block as? PrintBlock ?: return

    val hasLight = if (on) data.lightWhenOn else data.lightWhenOff
    val luminance = if (hasLight) data.lightLevel else 0

    val newState = cachedState
      .with(PrintBlock.on, !on)
      .with(PrintBlock.luminance, luminance)
    world.setBlockState(pos, newState, Block.NOTIFY_ALL)

    if (!data.isQuiet) {
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3f, if (on) 0.6f else 0.3f)
    }
    world.updateNeighborsAlways(pos, block)

    // If we're in button mode, turn off after 20 ticks
    if (on && data.isButton) {
      world.scheduleBlockTick(pos, block, block.toggleTicks)
    }
  }

  fun redstoneOutput(): Int {
    val emitting = if (on) emitsRedstoneWhenOn else emitsRedstoneWhenOff
    return if (emitting) data.redstoneLevel else 0
  }

  fun updateRedstoneInput() {
    val world = world ?: return
    if (world.isClient) return

    // If we're not an emitter, then the redstone input should toggle the print state
    val newOn = world.isReceivingRedstonePower(pos)
    if (!emitsRedstone && canTurnOn && on != newOn) {
      toggle()
    }
  }

  override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)
    nbt.optCompound("data")?.let { data = PrintData.fromNbt(it) }
  }

  override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)
    nbt.put("data", data.toNbt())
  }

  override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

  override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? =
    if (dataDirty) {
      dataDirty = false
      BlockEntityUpdateS2CPacket.create(this)
    } else {
      null
    }
}
