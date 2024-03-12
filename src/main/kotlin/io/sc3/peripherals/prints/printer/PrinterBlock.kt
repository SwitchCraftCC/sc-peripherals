package io.sc3.peripherals.prints.printer

import io.sc3.library.WaterloggableBlock
import io.sc3.library.WaterloggableBlock.Companion.waterlogged
import io.sc3.peripherals.Registration.ModBlockEntities.printer
import io.sc3.peripherals.util.BaseBlockWithEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

class PrinterBlock(settings: Settings) : BaseBlockWithEntity(settings), WaterloggableBlock {
  init {
    defaultState = defaultState
      .with(facing, Direction.NORTH)
      .with(waterlogged, false)
  }

  override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand,
                     hit: BlockHitResult): ActionResult {
    if (!world.isClient) {
      val factory = state.createScreenHandlerFactory(world, pos)
      factory?.let { player.openHandledScreen(it) }
    }

    return ActionResult.success(world.isClient)
  }

  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
    PrinterBlockEntity(pos, state)

  override fun <T : BlockEntity?> getTicker(
    world: World,
    state: BlockState,
    type: BlockEntityType<T>
  ): BlockEntityTicker<T>? {
    if (world.isClient) return null
    return checkType(type, printer, PrinterBlockEntity.Companion::onTick)
  }

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(facing, waterlogged)
  }

  override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
    state.rotate(mirror.getRotation(state.get(facing)))

  override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
    state.with(facing, rotation.rotate(state.get(facing)))

  override fun getPlacementState(ctx: ItemPlacementContext): BlockState = defaultState
    .with(facing, ctx.horizontalPlayerFacing.opposite)
    .with(waterlogged, placementWaterlogged(ctx))

  // Waterlogging
  override fun getFluidState(state: BlockState) = fluidState(state)
  override fun getStateForNeighborUpdate(state: BlockState, direction: Direction, neighborState: BlockState,
                                         world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
    neighborUpdate(state, world,  pos)
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
  }

  companion object {
    val facing: DirectionProperty = Properties.HORIZONTAL_FACING
  }
}
