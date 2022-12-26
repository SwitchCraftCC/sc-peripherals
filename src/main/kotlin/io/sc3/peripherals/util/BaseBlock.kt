package io.sc3.peripherals.util

import net.minecraft.block.Block
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.world.BlockView
import io.sc3.library.Tooltips.addDescLines

abstract class BaseBlock(settings: Settings) : Block(settings) {
  override fun appendTooltip(stack: ItemStack, world: BlockView?, tooltip: MutableList<Text>, options: TooltipContext) {
    super.appendTooltip(stack, world, tooltip, options)
    addDescLines(tooltip, translationKey)
  }
}
