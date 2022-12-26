package io.sc3.peripherals.datagen.recipes.handlers

import io.sc3.library.recipe.RecipeHandler

object RecipeHandlers {
  val RECIPE_HANDLERS by lazy { listOf(
    PrinterRecipes,
  )}

  @JvmStatic
  fun registerSerializers() {
    RECIPE_HANDLERS.forEach(RecipeHandler::registerSerializers)
  }
}
