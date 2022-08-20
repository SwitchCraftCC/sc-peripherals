package pw.switchcraft.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import org.slf4j.LoggerFactory

object ScPeripheralsDatagen : DataGeneratorEntrypoint {
  val log = LoggerFactory.getLogger("ScPeripherals/ScPeripheralsDatagen")!!

  override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
    log.info("sc-peripherals datagen initializing")

    generator.addProvider(RecipeGenerator(generator))
  }
}
