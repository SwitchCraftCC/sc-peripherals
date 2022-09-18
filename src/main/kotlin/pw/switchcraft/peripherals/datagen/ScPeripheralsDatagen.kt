package pw.switchcraft.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import org.slf4j.LoggerFactory
import pw.switchcraft.peripherals.datagen.recipes.RecipeGenerator

object ScPeripheralsDatagen : DataGeneratorEntrypoint {
  val log = LoggerFactory.getLogger("ScPeripherals/ScPeripheralsDatagen")!!

  override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
    log.info("sc-peripherals datagen initializing")

    generator.addProvider(BlockModelProvider(generator))
    generator.addProvider(ItemModelProvider(generator))
    generator.addProvider(RecipeGenerator(generator))
  }
}
