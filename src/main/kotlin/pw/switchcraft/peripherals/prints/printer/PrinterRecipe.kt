package pw.switchcraft.peripherals.prints.printer

import dan200.computercraft.api.ComputerCraftAPI
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags.IRON_INGOTS
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Ingredient.fromTag
import net.minecraft.recipe.Ingredient.ofItems
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import pw.switchcraft.library.recipe.BetterSpecialRecipe
import pw.switchcraft.peripherals.Registration.ModItems

class PrinterRecipe(id: Identifier) : BetterSpecialRecipe(id) {
  override val outputItem = ItemStack(ModItems.printer)

  private val ironIngot = fromTag(IRON_INGOTS)
  private val hopper = ofItems(HOPPER)
  private val stickyPiston = ofItems(STICKY_PISTON)
  private val diamondBlock = ofItems(DIAMOND_BLOCK)
  private val advancedComputer = ofItems(Registry.ITEM.get(Identifier(ComputerCraftAPI.MOD_ID, "computer_advanced")))

  override val ingredients = listOf(
    ironIngot,    hopper,           ironIngot,
    stickyPiston, diamondBlock,     stickyPiston,
    ironIngot,    advancedComputer, ironIngot
  )
  override val ingredientPredicates = listOf(
    Ingredient::test, Ingredient::test, Ingredient::test,
    Ingredient::test, Ingredient::test, Ingredient::test,
    Ingredient::test, ::testComputer,   Ingredient::test
  )

  // Ensure that the computer has no NBT (prevent accidental data loss)
  private fun testComputer(ingredient: Ingredient, stack: ItemStack) =
    advancedComputer.test(stack) && !stack.hasNbt()

  override fun isIgnoredInRecipeBook() = false
  override fun getSerializer() = recipeSerializer

  companion object {
    val recipeSerializer = SpecialRecipeSerializer { PrinterRecipe(it) }
  }
}
