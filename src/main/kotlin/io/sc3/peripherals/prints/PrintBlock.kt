package io.sc3.peripherals.prints

import io.sc3.library.WaterloggableBlock
import io.sc3.library.WaterloggableBlock.Companion.waterlogged
import io.sc3.peripherals.Registration.ModBlockEntities
import io.sc3.peripherals.Registration.ModBlocks
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.util.BaseBlockWithEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

class PrintBlock(settings: Settings) : BaseBlockWithEntity(settings), WaterloggableBlock {
  init {
    defaultState = defaultState
      .with(facing, Direction.NORTH)
      .with(on, false)
      .with(waterlogged, false)
      .with(luminance, 0)
  }

  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
    PrintBlockEntity(pos, state)

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(facing).add(on).add(waterlogged).add(luminance)
  }

  override fun mirror(state: BlockState, mirror: BlockMirror) =
    state.rotate(mirror.getRotation(state.get(facing)))

  override fun rotate(state: BlockState, rotation: BlockRotation) =
    state.with(facing, rotation.rotate(state.get(facing)))

  override fun getPlacementState(ctx: ItemPlacementContext): BlockState = defaultState
    .with(facing, ctx.horizontalPlayerFacing.opposite)
    .with(waterlogged, placementWaterlogged(ctx))
    .with(luminance, placementLuminance(ctx))

  override fun getPickStack(world: BlockView, pos: BlockPos, state: BlockState): ItemStack {
    val be = blockEntity(world, pos) ?: return ItemStack.EMPTY
    return PrintItem.fromBlockEntity(be)
  }

  override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
    super.onPlaced(world, pos, state, placer, stack)

    val be = blockEntity(world, pos) ?: return
    be.data = PrintItem.printData(stack)
    be.markDirty()
  }

  override fun getDroppedStacks(state: BlockState, builder: LootContextParameterSet.Builder): MutableList<ItemStack> {
    val be = builder.getOptional(LootContextParameters.BLOCK_ENTITY)
    if (be is PrintBlockEntity) {
      builder.addDynamicDrop(dropId) { consumer -> consumer.accept(PrintItem.fromBlockEntity(be)) }
    }

    return super.getDroppedStacks(state, builder)
  }

  // Shapes - outline and collision
  private fun getVoxelShape(state: BlockState, world: BlockView, pos: BlockPos): VoxelShape {
    val be = blockEntity(world, pos)
    val facing = state.get(facing)
    val on = state.get(on)
    return be?.data?.voxelShape(facing, on) ?: VoxelShapes.fullCube()
  }

  override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext) =
    getVoxelShape(state, world, pos)

  override fun getCollisionShape(state: BlockState, world: BlockView, pos: BlockPos,
                                 context: ShapeContext): VoxelShape {
    val shape = getVoxelShape(state, world, pos)
    val be = blockEntity(world, pos)
    return if (be?.collide == true) shape else VoxelShapes.empty()
  }

  override fun isShapeFullCube(state: BlockState, world: BlockView, pos: BlockPos) = false
  override fun isTransparent(state: BlockState, world: BlockView, pos: BlockPos) = true
  // override fun isSideInvisible(state: BlockState, stateFrom: BlockState, direction: Direction) = false // TODO

  // Toggling
  val toggleTicks = 20

  override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: Random) {
    if (world.isClient) return

    val be = blockEntity(world, pos) ?: return
    if (state.get(on)) be.toggle()
    if (state.get(on)) world.scheduleBlockTick(pos, this, toggleTicks)
  }

  override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand,
                     hit: BlockHitResult): ActionResult {
    val be = blockEntity(world, pos) ?: return super.onUse(state, world, pos, player, hand, hit)
    if (be.canTurnOn) be.toggle()
    return ActionResult.success(world.isClient)
  }

  // Light
  private fun placementLuminance(ctx: ItemPlacementContext): Int {
    val stack = ctx.stack
    if (!stack.isOf(ModItems.print)) return 0

    val data = PrintItem.printData(ctx.stack)
    return if (data?.lightWhenOff==true) data?.lightLevel ?: 0 else 0
  }

  // Redstone
  override fun emitsRedstonePower(state: BlockState) = true

  override fun getWeakRedstonePower(state: BlockState, world: BlockView, pos: BlockPos, side: Direction): Int =
    getStrongRedstonePower(state, world, pos, side)

  override fun getStrongRedstonePower(state: BlockState, world: BlockView, pos: BlockPos, side: Direction): Int =
    blockEntity(world, pos)?.redstoneOutput() ?: 0

  override fun neighborUpdate(state: BlockState, world: World, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos,
                              notify: Boolean) {
    super<BaseBlockWithEntity>.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
    super<WaterloggableBlock>.neighborUpdate(state, world, pos)
    blockEntity(world, pos)?.updateRedstoneInput()
  }

  // Waterlogging
  override fun getFluidState(state: BlockState) = fluidState(state)
  override fun getStateForNeighborUpdate(state: BlockState, direction: Direction, neighborState: BlockState,
                                         world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
    neighborUpdate(state, world,  pos)
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
  }

  companion object {
    val id = ModId("block/print")

    val dropId = ModId("print")

    val facing: DirectionProperty = Properties.HORIZONTAL_FACING
    val on: BooleanProperty = BooleanProperty.of("on")
    val luminance: IntProperty = IntProperty.of("luminance", 0, 15)

    private fun blockEntity(world: BlockView, pos: BlockPos): PrintBlockEntity? =
      world.getBlockEntity(pos, ModBlockEntities.print).orElse(null)

    @JvmStatic
    fun beaconBlockState(world: World, pos: BlockPos): BlockState {
      val state = world.getBlockState(pos)

      return if (state.isOf(ModBlocks.print)) {
        // If this is a beacon base block, pretend to be an iron block
        val be = blockEntity(world, pos)
        if (be?.data?.isBeaconBlock == true) {
          Blocks.IRON_BLOCK.defaultState
        } else {
          state
        }
      } else {
        state
      }
    }
  }
}
