package io.sc3.peripherals.prints

import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.recipe.Ingredient.ofItems
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.util.Identifier
import net.minecraft.world.World
import io.sc3.peripherals.Registration.ModItems

class PrintRecipe(
  id: Identifier,
  category: CraftingRecipeCategory = CraftingRecipeCategory.MISC
) : SpecialCraftingRecipe(id, category) {
  private val outputItem = ItemStack(ModItems.print)

  private val print = ofItems(ModItems.print)
  private val glowstone = ofItems(GLOWSTONE_DUST)
  private val beaconBlocks = ofItems(IRON_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK, EMERALD_BLOCK)

  private fun items(inv: CraftingInventory): RecipeItems? {
    val items = RecipeItems()

    for (i in 0 until inv.size()) {
      val stack = inv.getStack(i)
      if (stack.isEmpty) continue

      when {
        print.test(stack) -> {
          if (items.print != null) return null // Prevent item wastage with multiple prints
          items.print = stack
        }
        glowstone.test(stack) -> items.glowstone++
        beaconBlocks.test(stack) -> {
          if (items.beaconBlock != null) return null // Prevent item wastage with multiple beacon blocks
          items.beaconBlock = stack
        }
        else -> return null
      }
    }

    // Don't allow crafting if there's nothing valid to craft
    if (items.print == null || (items.glowstone <= 0 && items.beaconBlock == null)) {
      return null
    }

    // Don't allow crafting if the user would be wasting beacon blocks
    val data = PrintItem.printData(items.print!!) ?: return null
    if (data.isBeaconBlock && items.beaconBlock != null) {
      return null
    }

    // Don't allow crafting if the user would be wasting glowstone
    if (items.glowstone > 0 && items.glowstone + data.lightLevel > 15) {
      return null
    }

    return items
  }

  override fun matches(inv: CraftingInventory, world: World) = items(inv) != null

  override fun craft(inv: CraftingInventory): ItemStack {
    // Validate the crafting inputs and calculate what needs to be modified. Refuse to craft if any resources will be
    // wasted.
    val items = items(inv) ?: return ItemStack.EMPTY
    val print = items.print ?: return ItemStack.EMPTY

    val result = print.copy()
    result.count = 1

    // Get a fresh PrintData instance from the copy to mutate it
    val data = PrintItem.printData(result) ?: return ItemStack.EMPTY
    data.lightLevel = (data.lightLevel + items.glowstone).coerceIn(0, 15)
    if (items.beaconBlock != null) data.isBeaconBlock = true

    val nbt = result.nbt ?: return ItemStack.EMPTY
    nbt.put("data", data.toNbt())

    return result
  }

  override fun fits(width: Int, height: Int) = width * height >= 2
  override fun getSerializer() = recipeSerializer
  override fun getOutput() = outputItem
  override fun isIgnoredInRecipeBook() = true

  data class RecipeItems(
    var print: ItemStack? = null,
    var glowstone: Int = 0,
    var beaconBlock: ItemStack? = null
  )

  companion object {
    val recipeSerializer = SpecialRecipeSerializer(::PrintRecipe)
  }
}
