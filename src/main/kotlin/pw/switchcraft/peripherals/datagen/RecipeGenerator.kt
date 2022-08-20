package pw.switchcraft.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags.*
import net.minecraft.advancement.criterion.InventoryChangedCriterion
import net.minecraft.data.server.recipe.ComplexRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items.*
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import pw.switchcraft.peripherals.Registration.ModItems
import pw.switchcraft.peripherals.ScPeripherals.ModId
import pw.switchcraft.peripherals.prints.PrintRecipe
import pw.switchcraft.peripherals.prints.printer.PrinterRecipe
import java.util.function.Consumer
import dan200.computercraft.shared.Registry as CcRegistry

class RecipeGenerator(generator: FabricDataGenerator) : FabricRecipeProvider(generator) {
  override fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
    // Printers
    BetterComplexRecipeJsonBuilder(ModItems.printer, PrinterRecipe.recipeSerializer)
      .criterion("has_advanced_computer", inventoryChange(CcRegistry.ModItems.COMPUTER_ADVANCED))
      .offerTo(exporter, ModId("printer"))

    // 3D prints (glowstone, beacon blocks)
    specialRecipe(exporter, PrintRecipe.recipeSerializer)

    // Chamelium
    @Suppress("RemoveRedundantQualifierName")
    ShapedRecipeJsonBuilder
      .create(ModItems.chamelium, 16)
      .pattern("GRG")
      .pattern("RCR")
      .pattern("GWG")
      .input('G', GRAVEL)
      .input('R', REDSTONE_DUSTS)
      .input('C', ConventionalItemTags.COAL)
      .input('W', WATER_BUCKETS)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter)

    // Ink cartridges
    ShapedRecipeJsonBuilder
      .create(ModItems.inkCartridge, 1)
      .pattern("CMY")
      .pattern("KDB")
      .pattern("III")
      .input('C', CYAN_DYES)
      .input('M', MAGENTA_DYES)
      .input('Y', YELLOW_DYES)
      .input('K', BLACK_DYES)
      .input('D', DISPENSER)
      .input('B', BUCKET)
      .input('I', IRON_INGOTS)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter, ModId("ink_cartridge"))

    ShapelessRecipeJsonBuilder
      .create(ModItems.inkCartridge, 1)
      .input(ModItems.emptyInkCartridge)
      .input(CYAN_DYES)
      .input(MAGENTA_DYES)
      .input(YELLOW_DYES)
      .input(BLACK_DYES)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter, ModId("ink_cartridge_refill"))

    ShapedRecipeJsonBuilder
      .create(ModItems.textureAnalyzer, 1)
      .pattern("GPE")
      .pattern("RRC")
      .pattern("MYK")
      .input('G', GOLD_BLOCK)
      .input('P', GLASS_PANES)
      .input('E', ENDER_EYE)
      .input('R', REDSTONE_DUSTS)
      .input('C', CYAN_DYES)
      .input('M', MAGENTA_DYES)
      .input('Y', YELLOW_DYES)
      .input('K', BLACK_DYES)
      .criterion("has_printer", inventoryChange(ModItems.printer))
      .offerTo(exporter)
  }

  companion object {
    private fun inventoryChange(vararg items: ItemConvertible) = InventoryChangedCriterion.Conditions.items(*items)!!

    private fun <T : Recipe<C>, C : Inventory> specialRecipe(
      exporter: Consumer<RecipeJsonProvider>,
      serializer: SpecialRecipeSerializer<T>
    ): Identifier {
      val recipeId = Registry.RECIPE_SERIALIZER.getId(serializer)
        ?: throw IllegalStateException("Recipe serializer $serializer is not registered")
      ComplexRecipeJsonBuilder.create(serializer).offerTo(exporter, recipeId.toString())
      return recipeId
    }
  }
}
