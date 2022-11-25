package pw.switchcraft.peripherals.datagen.recipes.handlers

import dan200.computercraft.api.ComputerCraftTags
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries.RECIPE_SERIALIZER
import net.minecraft.registry.Registry.register
import pw.switchcraft.library.recipe.BetterComplexRecipeJsonBuilder
import pw.switchcraft.library.recipe.RecipeHandler
import pw.switchcraft.peripherals.Registration.ModItems
import pw.switchcraft.peripherals.ScPeripherals.ModId
import pw.switchcraft.peripherals.datagen.recipes.inventoryChange
import pw.switchcraft.peripherals.prints.PrintRecipe
import pw.switchcraft.peripherals.prints.printer.PrinterRecipe
import java.util.function.Consumer

object PrinterRecipes : RecipeHandler {
  override fun registerSerializers() {
    register(RECIPE_SERIALIZER, ModId("printer"), PrinterRecipe.recipeSerializer)
    register(RECIPE_SERIALIZER, ModId("print"), PrintRecipe.recipeSerializer)
  }

  override fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
    // Printers
    BetterComplexRecipeJsonBuilder(ModItems.printer, PrinterRecipe.recipeSerializer)
      .hasComputer()
      .offerTo(exporter)

    // 3D prints (glowstone, beacon blocks)
    BetterComplexRecipeJsonBuilder(ModItems.print, PrintRecipe.recipeSerializer)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .criterion("has_print", inventoryChange(ModItems.print))
      .offerTo(exporter)

    // Chamelium
    ShapedRecipeJsonBuilder
      .create(RecipeCategory.MISC, ModItems.chamelium, 16)
      .pattern("GRG")
      .pattern("RCR")
      .pattern("GWG")
      .input('G', Items.GRAVEL)
      .input('R', ConventionalItemTags.REDSTONE_DUSTS)
      .input('C', ConventionalItemTags.COAL)
      .input('W', ConventionalItemTags.WATER_BUCKETS)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter)

    // Ink cartridges
    ShapedRecipeJsonBuilder
      .create(RecipeCategory.MISC, ModItems.inkCartridge, 1)
      .pattern("CMY")
      .pattern("KDB")
      .pattern("III")
      .input('C', ConventionalItemTags.CYAN_DYES)
      .input('M', ConventionalItemTags.MAGENTA_DYES)
      .input('Y', ConventionalItemTags.YELLOW_DYES)
      .input('K', ConventionalItemTags.BLACK_DYES)
      .input('D', Items.DISPENSER)
      .input('B', Items.BUCKET)
      .input('I', ConventionalItemTags.IRON_INGOTS)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter, ModId("ink_cartridge"))

    ShapelessRecipeJsonBuilder
      .create(RecipeCategory.MISC, ModItems.inkCartridge, 1)
      .input(ModItems.emptyInkCartridge)
      .input(ConventionalItemTags.CYAN_DYES)
      .input(ConventionalItemTags.MAGENTA_DYES)
      .input(ConventionalItemTags.YELLOW_DYES)
      .input(ConventionalItemTags.BLACK_DYES)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter, ModId("ink_cartridge_refill"))

    ShapedRecipeJsonBuilder
      .create(RecipeCategory.MISC, ModItems.textureAnalyzer, 1)
      .pattern("GPE")
      .pattern("RRC")
      .pattern("MYK")
      .input('G', Items.GOLD_BLOCK)
      .input('P', ConventionalItemTags.GLASS_PANES)
      .input('E', Items.ENDER_EYE)
      .input('R', ConventionalItemTags.REDSTONE_DUSTS)
      .input('C', ConventionalItemTags.CYAN_DYES)
      .input('M', ConventionalItemTags.MAGENTA_DYES)
      .input('Y', ConventionalItemTags.YELLOW_DYES)
      .input('K', ConventionalItemTags.BLACK_DYES)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter)
  }

  private val computerCriteria = mapOf(
    "has_computer" to inventoryChange(ComputerCraftTags.Items.COMPUTER),
    "has_turtle" to inventoryChange(ComputerCraftTags.Items.TURTLE)
  )

  private fun BetterComplexRecipeJsonBuilder<*>.hasComputer() = apply {
    computerCriteria.forEach { criterion(it.key, it.value) }
  }
}
