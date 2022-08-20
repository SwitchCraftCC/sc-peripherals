package pw.switchcraft.peripherals.prints.printer

import dan200.computercraft.shared.Registry
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags.IRON_INGOTS
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Ingredient.fromTag
import net.minecraft.recipe.Ingredient.ofItems
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
import pw.switchcraft.peripherals.Registration.ModItems

private const val WIDTH = 3
private const val HEIGHT = 3

class PrinterRecipe(id: Identifier) : SpecialCraftingRecipe(id) {
  private val outputItem = ItemStack(ModItems.printer)

  private val ironIngot = fromTag(IRON_INGOTS)
  private val hopper = ofItems(HOPPER)
  private val stickyPiston = ofItems(STICKY_PISTON)
  private val diamondBlock = ofItems(DIAMOND_BLOCK)
  private val advancedComputer = ofItems(Registry.ModItems.COMPUTER_ADVANCED)

  private val ingredientPattern = listOf(
    ironIngot,    hopper,           ironIngot,
    stickyPiston, diamondBlock,     stickyPiston,
    ironIngot,    advancedComputer, ironIngot
  )
  private val ingredientPredicates = listOf(
    ironIngot::test,    hopper::test,       ironIngot::test,
    stickyPiston::test, diamondBlock::test, stickyPiston::test,
    ironIngot::test,    ::testComputer,     ironIngot::test
  )

  // Ensure that the computer has no NBT (prevent accidental data loss)
  private fun testComputer(stack: ItemStack) =
    advancedComputer.test(stack) && !stack.hasNbt()

  override fun matches(inv: CraftingInventory, world: World): Boolean {
    if (!fits(inv.width, inv.height)) return false

    for (i in 0 until WIDTH) {
      for (j in 0 until HEIGHT) {
        val stack = inv.getStack(i + j * inv.width)
        if (!ingredientPredicates[i + j * WIDTH].invoke(stack)) return false
      }
    }

    return true
  }

  override fun craft(inventory: CraftingInventory): ItemStack = outputItem.copy()
  override fun fits(width: Int, height: Int) = width >= WIDTH && height >= HEIGHT
  override fun getSerializer() = recipeSerializer
  override fun getOutput() = outputItem
  override fun isIgnoredInRecipeBook() = false
  override fun getIngredients(): DefaultedList<Ingredient> =
    DefaultedList.copyOf(null, *ingredientPattern.toTypedArray())

  companion object {
    val recipeSerializer = SpecialRecipeSerializer { PrinterRecipe(it) }
  }
}
