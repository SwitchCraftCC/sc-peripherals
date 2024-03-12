package io.sc3.peripherals.prints

import io.sc3.library.ext.optCompound
import io.sc3.peripherals.Registration.ModBlocks
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.ScPeripherals.modId
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.Text.literal
import net.minecraft.text.Text.translatable
import net.minecraft.util.Formatting.GRAY
import net.minecraft.world.World

class PrintItem(settings: Settings) : BlockItem(ModBlocks.print, settings) {
  override fun getName(stack: ItemStack): Text {
    return printData(stack).labelText ?: return super.getName(stack)
  }

  override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
    // Don't call super here
    val data = printData(stack)
    data.tooltip?.let { tooltip.add(literal(it)) }

    fun line(key: String, vararg args: Any) {
      tooltip.add(translatable("block.$modId.print.$key", *args).formatted(GRAY))
    }

    if (data.isBeaconBlock) line("beacon_base")
    if (data.isQuiet) line("quiet")
    if (data.redstoneLevel > 0) line("redstone_level", data.redstoneLevel)
    if (data.lightLevel > 0) line("light_level", data.lightLevel)
  }

  companion object {
    val id = ModId("item/print")

    fun printData(stack: ItemStack): PrintData =
      stack.orCreateNbt.optCompound("data")?.let { PrintData.fromNbt(it) } ?: PrintData()

    fun fromBlockEntity(be: PrintBlockEntity): ItemStack = ItemStack(ModItems.print).apply {
      val data = be.data
      orCreateNbt.put("data", data.toNbt())
    }

    fun create(data: PrintData): ItemStack = ItemStack(ModItems.print).apply {
      orCreateNbt.put("data", data.toNbt())
    }

    @JvmStatic
    fun hasCustomName(stack: ItemStack) =
      printData(stack).labelText != null

    @JvmStatic
    fun getCustomName(stack: ItemStack): Text =
      stack.item.getName(stack)

    @JvmStatic
    fun setCustomName(stack: ItemStack, name: Text?) {
      val data = stack.getSubNbt("data") ?: return
      val newLabel = PrintData.sanitiseLabel(name?.string)
      if (newLabel == null) {
        data.remove("label") // TODO: Make NbtExt.putOptString remove the key if it is present
      } else {
        data.putString("label", newLabel)
      }
    }

    @JvmStatic
    fun removeCustomName(stack: ItemStack) {
      val data = stack.getSubNbt("data") ?: return
      data.remove("label")
    }
  }
}
