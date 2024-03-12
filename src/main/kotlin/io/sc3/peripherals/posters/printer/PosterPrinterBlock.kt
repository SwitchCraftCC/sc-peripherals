package io.sc3.peripherals.posters.printer

import io.sc3.library.WaterloggableBlock
import io.sc3.library.WaterloggableBlock.Companion.waterlogged
import io.sc3.library.ext.rotateTowards
import io.sc3.library.ext.toDiv16VoxelShape
import io.sc3.library.ext.toMul16
import io.sc3.peripherals.Registration.ModBlockEntities.posterPrinter
import io.sc3.peripherals.util.BaseBlockWithEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

class PosterPrinterBlock(settings: Settings) : BaseBlockWithEntity(settings), WaterloggableBlock {
  init {
    defaultState = defaultState
      .with(facing, Direction.NORTH)
      .with(waterlogged, false)
      .with(printing, false)
      .with(hasPaper, false)
  }

  override fun getOutlineShape(
    state: BlockState?,
    world: BlockView?,
    pos: BlockPos?,
    context: ShapeContext?
  ): VoxelShape = voxelShapes[state?.get(facing) ?: Direction.NORTH]!!

  override fun getRenderType(state: BlockState?) = BlockRenderType.ENTITYBLOCK_ANIMATED

  override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand,
                     hit: BlockHitResult): ActionResult {
    if (!world.isClient) {
      val factory = state.createScreenHandlerFactory(world, pos)
      factory?.let { player.openHandledScreen(it) }
    }

    return ActionResult.success(world.isClient)
  }

  override fun createBlockEntity(pos: BlockPos, state: BlockState) =
    PosterPrinterBlockEntity(pos, state)

  override fun <T : BlockEntity?> getTicker(
    world: World,
    state: BlockState,
    type: BlockEntityType<T>
  ): BlockEntityTicker<T>? {
    return if (world.isClient) {
      checkType(type, posterPrinter, PosterPrinterBlockEntity.Companion::onClientTick)
    } else {
      checkType(type, posterPrinter, PosterPrinterBlockEntity.Companion::onTick)
    }
  }

  override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
    builder.add(facing, waterlogged, printing, hasPaper)
  }

  override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
    state.rotate(mirror.getRotation(state.get(facing)))

  override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
    state.with(facing, rotation.rotate(state.get(facing)))

  override fun getPlacementState(ctx: ItemPlacementContext): BlockState = defaultState
    .with(facing, ctx.horizontalPlayerFacing.opposite)
    .with(waterlogged, placementWaterlogged(ctx))
    .with(printing, false)
    .with(hasPaper, false)

  // Waterlogging
  override fun getFluidState(state: BlockState) = fluidState(state)
  override fun getStateForNeighborUpdate(state: BlockState, direction: Direction, neighborState: BlockState,
                                         world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
    neighborUpdate(state, world,  pos)
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
  }

  companion object {
    val facing: DirectionProperty = Properties.HORIZONTAL_FACING
    val printing: BooleanProperty = BooleanProperty.of("printing")
    val hasPaper: BooleanProperty = BooleanProperty.of("has_paper")

    private val cuboids = listOf(
      VoxelShapes.cuboid(0.0, 0.0, 0.25, 1.0, 0.125, 1.0),
      VoxelShapes.cuboid(0.125, 0.0625, 0.0, 0.875, 0.125, 0.25),
      VoxelShapes.cuboid(0.0, 0.125, 0.5625, 1.0, 0.625, 1.0),
      VoxelShapes.cuboid(0.0, 0.125, 0.4375, 0.125, 0.25, 0.5625),
      VoxelShapes.cuboid(0.875, 0.125, 0.4375, 1.0, 0.25, 0.5625),
      VoxelShapes.cuboid(0.0, 0.25, 0.4375, 1.0, 0.5, 0.5625),
      VoxelShapes.cuboid(0.125, 0.625, 0.9375, 0.875, 1.0, 1.0),
    )

    private val voxelShapes = listOf(
      Direction.NORTH,
      Direction.EAST,
      Direction.SOUTH,
      Direction.WEST
    ).associateWith(::makeShapesForDirection)

    private fun makeShapesForDirection(direction: Direction): VoxelShape = cuboids
      .map { it.boundingBox.toMul16().rotateTowards(direction).toDiv16VoxelShape() }
      .reduce { acc, shape -> VoxelShapes.union(acc, shape) }
  }
}
