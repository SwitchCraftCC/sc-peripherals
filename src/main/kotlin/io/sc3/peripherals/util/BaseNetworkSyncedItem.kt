package io.sc3.peripherals.util

import io.sc3.library.Tooltips.addDescLines
import io.sc3.peripherals.ScPeripherals.modId
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.item.NetworkSyncedItem
import net.minecraft.text.Text
import net.minecraft.world.World

abstract class BaseNetworkSyncedItem(
  private val itemName: String,
  settings: Settings
) : NetworkSyncedItem(settings) {
  override fun getTranslationKey() = itemTranslationKey(itemName)

  override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
    super.appendTooltip(stack, world, tooltip, context)
    addDescLines(tooltip, getTranslationKey(stack))
  }

  companion object {
    fun itemTranslationKey(name: String) = "item.$modId.$name"
  }
}
