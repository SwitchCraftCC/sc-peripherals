package pw.switchcraft.peripherals.util

import net.minecraft.block.BlockState
import net.minecraft.block.Waterloggable
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids.EMPTY
import net.minecraft.fluid.Fluids.WATER
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

interface WaterloggableBlock : Waterloggable {
  fun fluidState(state: BlockState): FluidState =
    if (state.get(waterlogged)) WATER.getStill(false) else EMPTY.defaultState

  fun placementWaterlogged(ctx: ItemPlacementContext): Boolean =
    ctx.world.getFluidState(ctx.blockPos).fluid == WATER

  fun neighborUpdate(state: BlockState, world: WorldAccess, pos: BlockPos) {
    if (state.get(waterlogged)) {
      world.createAndScheduleFluidTick(pos, WATER, WATER.getTickRate(world))
    }
  }

  companion object {
    val waterlogged: BooleanProperty = Properties.WATERLOGGED
  }
}
