package pw.switchcraft.peripherals.prints

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.Text.literal
import net.minecraft.text.Text.translatable
import net.minecraft.util.Formatting.GRAY
import net.minecraft.world.World
import pw.switchcraft.library.ext.optCompound
import pw.switchcraft.peripherals.Registration.ModBlocks
import pw.switchcraft.peripherals.Registration.ModItems
import pw.switchcraft.peripherals.ScPeripherals.ModId
import pw.switchcraft.peripherals.ScPeripherals.modId

class PrintItem(settings: Settings) : BlockItem(ModBlocks.print, settings) {
  override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
    // Don't call super here
    val data = printData(stack) ?: return
    data.tooltip?.let { tooltip.add(literal(it)) }

    fun line(key: String, vararg args: Any) {
      tooltip.add(translatable("block.$modId.print.$key", *args).formatted(GRAY))
    }

    if (data.isBeaconBlock) line("beacon_base")
    if (data.redstoneLevel > 0) line("redstone_level", data.redstoneLevel)
    if (data.lightLevel > 0) line("light_level", data.lightLevel)
  }

  companion object {
    val id = ModId("item/print")

    fun printData(stack: ItemStack): PrintData? =
      stack.orCreateNbt.optCompound("data")?.let { PrintData.fromNbt(it) }

    fun fromBlockEntity(be: PrintBlockEntity): ItemStack = ItemStack(ModItems.print).apply {
      val data = be.data ?: return ItemStack.EMPTY
      orCreateNbt.put("data", data.toNbt())
    }

    fun create(data: PrintData): ItemStack = ItemStack(ModItems.print).apply {
      orCreateNbt.put("data", data.toNbt())
    }
  }
}
