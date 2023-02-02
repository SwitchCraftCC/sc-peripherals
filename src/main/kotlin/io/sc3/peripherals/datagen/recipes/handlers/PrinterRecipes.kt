package io.sc3.peripherals.datagen.recipes.handlers

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.ComputerCraftTags
import io.sc3.library.recipe.BetterComplexRecipeJsonBuilder
import io.sc3.library.recipe.RecipeHandler
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.datagen.recipes.inventoryChange
import io.sc3.peripherals.prints.PrintRecipe
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags
import net.minecraft.data.server.recipe.CraftingRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.Registries.RECIPE_SERIALIZER
import net.minecraft.registry.Registry.register
import net.minecraft.util.Identifier
import java.util.function.Consumer

object PrinterRecipes : RecipeHandler {
  override fun registerSerializers() {
    register(RECIPE_SERIALIZER, ModId("print"), PrintRecipe.recipeSerializer)
  }

  override fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
    // Printers
    ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.printer)
      .pattern("IHI")
      .pattern("PDP")
      .pattern("ICI")
      .input('I', ConventionalItemTags.IRON_INGOTS)
      .input('H', Items.HOPPER)
      .input('P', Items.STICKY_PISTON)
      .input('D', Items.DIAMOND_BLOCK)
      .input(
        'C', DefaultCustomIngredients.nbt(
          ItemStack(Registries.ITEM.get(Identifier(ComputerCraftAPI.MOD_ID, "computer_advanced"))),
          true
        )
      )
      .hasComputer()
      .offerTo(exporter)

    ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.posterPrinter)
      .pattern("SNS")
      .pattern("IDI")
      .pattern("QCQ")
      .input('I', ConventionalItemTags.IRON_INGOTS)
      .input('Q', Items.QUARTZ_BLOCK)
      .input('N', Items.NETHER_STAR)
      .input('S', Items.STICK)
      .input('D', Items.DIAMOND_BLOCK)
      .input(
        'C', DefaultCustomIngredients.nbt(
          ItemStack(Registries.ITEM.get(Identifier(ComputerCraftAPI.MOD_ID, "computer_advanced"))),
          true
        )
      )
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

  private fun CraftingRecipeJsonBuilder.hasComputer() = apply {
    computerCriteria.forEach { criterion(it.key, it.value) }
  }
}
