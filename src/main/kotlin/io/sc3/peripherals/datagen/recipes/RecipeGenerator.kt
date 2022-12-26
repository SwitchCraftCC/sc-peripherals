package io.sc3.peripherals.datagen.recipes

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.advancement.criterion.InventoryChangedCriterion
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.registry.tag.TagKey
import io.sc3.peripherals.datagen.recipes.handlers.RecipeHandlers.RECIPE_HANDLERS
import java.util.function.Consumer

class RecipeGenerator(out: FabricDataOutput) : FabricRecipeProvider(out) {
  override fun generate(exporter: Consumer<RecipeJsonProvider>) {
    RECIPE_HANDLERS.forEach { it.generateRecipes(exporter) }
  }
}

fun inventoryChange(vararg items: ItemConvertible): InventoryChangedCriterion.Conditions =
  InventoryChangedCriterion.Conditions.items(*items)

fun inventoryChange(tag: TagKey<Item>): InventoryChangedCriterion.Conditions =
  InventoryChangedCriterion.Conditions.items(ItemPredicate.Builder.create().tag(tag).build())
