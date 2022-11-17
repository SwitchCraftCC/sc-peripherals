package pw.switchcraft.peripherals.datagen.recipes

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.advancement.criterion.InventoryChangedCriterion
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.tag.TagKey
import pw.switchcraft.peripherals.datagen.recipes.handlers.RecipeHandlers.RECIPE_HANDLERS
import java.util.function.Consumer

class RecipeGenerator(generator: FabricDataGenerator) : FabricRecipeProvider(generator) {
  override fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
    RECIPE_HANDLERS.forEach { it.generateRecipes(exporter) }
  }
}

fun inventoryChange(vararg items: ItemConvertible): InventoryChangedCriterion.Conditions =
  InventoryChangedCriterion.Conditions.items(*items)

fun inventoryChange(tag: TagKey<Item>): InventoryChangedCriterion.Conditions =
  InventoryChangedCriterion.Conditions.items(ItemPredicate.Builder.create().tag(tag).build())
