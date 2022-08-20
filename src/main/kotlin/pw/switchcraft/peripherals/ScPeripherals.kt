package pw.switchcraft.peripherals

import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import pw.switchcraft.peripherals.config.ScPeripheralsConfig

object ScPeripherals : ModInitializer {
  val log = LoggerFactory.getLogger("ScPeripherals")!!

  val modId = "sc-peripherals"
  fun ModId(value: String) = Identifier(modId, value)

  override fun onInitialize() {
    log.info("sc-peripherals initializing")

    // Initialize the default config file if it does not yet exist
    ScPeripheralsConfig.config.load()

    Registration.init()
  }
}
