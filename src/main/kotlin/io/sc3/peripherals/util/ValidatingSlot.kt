package io.sc3.peripherals.util

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

class ValidatingSlot(
  inv: Inventory,
  index: Int,
  x: Int,
  y: Int,
  val predicate: (ItemStack) -> Boolean
): Slot(inv, index, x, y) {
  override fun canInsert(stack: ItemStack) = predicate(stack)
}
