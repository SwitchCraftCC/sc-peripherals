package pw.switchcraft.peripherals.datagen

import com.google.gson.JsonObject
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementRewards
import net.minecraft.advancement.CriterionMerger
import net.minecraft.advancement.criterion.CriterionConditions
import net.minecraft.advancement.criterion.RecipeUnlockedCriterion
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemConvertible
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.SpecialRecipeSerializer
import net.minecraft.util.Identifier
import java.util.function.Consumer

class BetterComplexRecipeJsonBuilder<T : Recipe<U>, U : Inventory>(
  output: ItemConvertible,
  private val specialSerializer: SpecialRecipeSerializer<T>
) {
  private val outputItem = output.asItem()
  private val advancementBuilder: Advancement.Builder = Advancement.Builder.create()

  fun criterion(name: String, conditions: CriterionConditions) = apply {
    advancementBuilder.criterion(name, conditions)
  }

  fun offerTo(exporter: Consumer<RecipeJsonProvider>, recipeId: Identifier) {
    val advancementId = Identifier(recipeId.namespace, "recipes/" +
      (outputItem.group?.name ?: "item") + "/" + recipeId.path)
    val advancement = advancementBuilder
      .parent(Identifier("recipes/root")) // TODO: PR a name to yarn for field_39377
      .criterion("has_the_recipe", RecipeUnlockedCriterion.create(recipeId))
      .rewards(AdvancementRewards.Builder.recipe(recipeId))
      .criteriaMerger(CriterionMerger.OR)
      .toJson()

    exporter.accept(object : RecipeJsonProvider {
      override fun serialize(json: JsonObject) {} // No-op
      override fun getRecipeId() = recipeId
      override fun getSerializer() = specialSerializer
      override fun toAdvancementJson() = advancement
      override fun getAdvancementId() = advancementId
    })
  }
}
