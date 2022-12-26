package io.sc3.peripherals.util

import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import io.sc3.library.Tooltips.addDescLines

abstract class BaseBlockWithEntity(settings: Settings) : BlockWithEntity(settings) {
  override fun appendTooltip(stack: ItemStack, world: BlockView?, tooltip: MutableList<Text>, options: TooltipContext) {
    super.appendTooltip(stack, world, tooltip, options)
    addDescLines(tooltip, translationKey)
  }

  override fun getRenderType(state: BlockState?) = BlockRenderType.MODEL

  override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
    if (state.isOf(newState.block)) return

    val be = world.getBlockEntity(pos)
    if (be is BaseBlockEntity) be.onBroken()

    super.onStateReplaced(state, world, pos, newState, moved)
  }
}
