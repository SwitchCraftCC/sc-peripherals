package io.sc3.peripherals.prints

import io.sc3.library.ext.*
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
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

class PrintBlockEntity(
  pos: BlockPos,
  state: BlockState
) : BaseBlockEntity(print, pos, state) {
  private var dataDirty = false
  var data: PrintData? = null
    set(value) {
      field = value
      dataDirty = true
    }

  val on
    get() = with(cachedState) { if (isAir) false else get(PrintBlock.on) }
  val facing: Direction
    get() = with(cachedState) { if (isAir) Direction.NORTH else get(PrintBlock.facing) }

  val canTurnOn
    get() = data?.shapesOn?.isNotEmpty() ?: false
  val shapes
    get() = data?.let { if (on) it.shapesOn else it.shapesOff }

  val collide
    get() = data?.let { if (on) it.collideWhenOn else it.collideWhenOff }

  private val emitsRedstone
    get() = (data?.redstoneLevel ?: 0) > 0
  private val emitsRedstoneWhenOff
    get() = emitsRedstone && !canTurnOn
  private val emitsRedstoneWhenOn
    get() = emitsRedstone && canTurnOn

  private fun boundsForState(shapes: Shapes): Box {
    val head = shapes.firstOrNull()?.bounds ?: unitBox
    val bounds = shapes.drop(1).fold(head) { acc, shape -> acc.union(shape.bounds) }
    return if (bounds.volume == 0) unitBox
    else bounds.rotateTowards(facing)
  }

  fun toggle() {
    val world = world ?: return
    val block = cachedState.block as? PrintBlock ?: return

    world.setBlockState(pos, cachedState.with(PrintBlock.on, !on), Block.NOTIFY_ALL)
    if (data?.isQuiet != false) {
      world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3f, if (on) 0.6f else 0.3f)
    }
    world.updateNeighborsAlways(pos, block)

    // If we're in button mode, turn off after 20 ticks
    if (on && data?.isButton == true) {
      world.scheduleBlockTick(pos, block, block.toggleTicks)
    }
  }

  // TODO: This is no longer needed - Minecraft does this calculation natively?
  fun isSideSolid(side: Direction) =
    shapes?.filter { it.texture != null }?.forEach {
      val bounds = it.bounds.rotateTowards(facing)
      val fullX = bounds.minX == 0.0 && bounds.maxX == 1.0
      val fullY = bounds.minY == 0.0 && bounds.maxY == 1.0
      val fullZ = bounds.minZ == 0.0 && bounds.maxZ == 1.0
      return when (side) {
        Direction.DOWN -> bounds.minY == 0.0 && fullX && fullZ
        Direction.UP -> bounds.maxY == 1.0 && fullX && fullZ
        Direction.NORTH -> bounds.minZ == 0.0 && fullX && fullY
        Direction.SOUTH -> bounds.maxZ == 1.0 && fullX && fullY
        Direction.WEST -> bounds.minX == 0.0 && fullY && fullZ
        Direction.EAST -> bounds.maxX == 1.0 && fullY && fullZ
      }
    } ?: false

  fun redstoneOutput(): Int {
    val emitting = if (on) emitsRedstoneWhenOn else emitsRedstoneWhenOff
    return if (emitting) data?.redstoneLevel ?: 0 else 0
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
    nbt.putNullableCompound("data", data?.toNbt())
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
