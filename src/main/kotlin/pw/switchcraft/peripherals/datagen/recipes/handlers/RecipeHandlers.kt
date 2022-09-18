package pw.switchcraft.peripherals.datagen.recipes.handlers

import pw.switchcraft.library.recipe.RecipeHandler

object RecipeHandlers {
  val RECIPE_HANDLERS by lazy { listOf(
    PrinterRecipes,
  )}

  @JvmStatic
  fun registerSerializers() {
    RECIPE_HANDLERS.forEach(RecipeHandler::registerSerializers)
  }
}
