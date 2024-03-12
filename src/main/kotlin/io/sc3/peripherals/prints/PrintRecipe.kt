package io.sc3.peripherals.prints

import io.sc3.peripherals.Registration.ModItems
import net.minecraft.inventory.RecipeInputInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.recipe.Ingredient.ofItems
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.util.Identifier
import net.minecraft.world.World

class PrintRecipe(
  id: Identifier,
  category: CraftingRecipeCategory = CraftingRecipeCategory.MISC
) : SpecialCraftingRecipe(id, category) {
  private val outputItem = ItemStack(ModItems.print)

  private val print = ofItems(ModItems.print)
  private val glowstoneDust = ofItems(GLOWSTONE_DUST)
  private val glowstoneBlock = ofItems(GLOWSTONE)
  private val beaconBlocks = ofItems(IRON_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK, EMERALD_BLOCK)
  private val honeyBlock = ofItems(HONEY_BLOCK)

  private fun items(inv: RecipeInputInventory): RecipeItems? {
    val items = RecipeItems()

    for (i in 0 until inv.size()) {
      val stack = inv.getStack(i)
      if (stack.isEmpty) continue

      when {
        print.test(stack) -> {
          if (items.print != null) return null // Prevent item wastage with multiple prints
          items.print = stack
        }
        glowstoneDust.test(stack) -> items.lightIncrease++
        glowstoneBlock.test(stack) -> items.lightIncrease+= 4 // Glowstone blocks are approximately "worth" 4 glowstone dust
        beaconBlocks.test(stack) -> {
          if (items.beaconBlock != null) return null // Prevent item wastage with multiple beacon blocks
          items.beaconBlock = stack
        }
        honeyBlock.test(stack) -> {
          if (items.honeyBlock != null) return null // Prevent item wastage with multiple honey blocks
          items.honeyBlock = stack
        }
        else -> return null
      }
    }

    // Don't allow crafting if there's nothing valid to craft
    if (items.print == null || (items.lightIncrease <= 0 && items.beaconBlock == null && items.honeyBlock == null)) {
      return null
    }

    // Don't allow crafting if the user would be wasting beacon or honey blocks
    val data = PrintItem.printData(items.print!!)
    if (data.isBeaconBlock && items.beaconBlock != null) {
      return null
    }
    if (data.isQuiet && items.honeyBlock != null) {
      return null
    }

    // Don't allow crafting if the user would be wasting glowstone
    if (items.lightIncrease > 0 && items.lightIncrease + data.lightLevel > 15) {
      return null
    }

    return items
  }

  override fun matches(inv: RecipeInputInventory, world: World) =
    items(inv) != null

  override fun craft(inv: RecipeInputInventory, manager: DynamicRegistryManager): ItemStack {
    // Validate the crafting inputs and calculate what needs to be modified. Refuse to craft if any resources will be
    // wasted.
    val items = items(inv) ?: return ItemStack.EMPTY
    val print = items.print ?: return ItemStack.EMPTY

    val result = print.copyWithCount(1)

    // Get a fresh PrintData instance from the copy to mutate it
    val data = PrintItem.printData(result)
    data.lightLevel = (data.lightLevel + items.lightIncrease).coerceIn(0, 15)
    if (items.beaconBlock != null) data.isBeaconBlock = true
    if (items.honeyBlock != null) data.isQuiet = true

    val nbt = result.nbt ?: return ItemStack.EMPTY
    nbt.put("data", data.toNbt())

    return result
  }

  override fun fits(width: Int, height: Int) = width * height >= 2
  override fun getSerializer() = recipeSerializer
  override fun getOutput(manager: DynamicRegistryManager) = outputItem
  override fun isIgnoredInRecipeBook() = true

  data class RecipeItems(
    var print: ItemStack? = null,
    var lightIncrease: Int = 0,
    var beaconBlock: ItemStack? = null,
    var honeyBlock: ItemStack? = null
  )

  companion object {
    val recipeSerializer = SpecialRecipeSerializer(::PrintRecipe)
  }
}
