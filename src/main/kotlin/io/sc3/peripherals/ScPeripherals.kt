package io.sc3.peripherals

import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import io.sc3.peripherals.config.ScPeripheralsConfig

object ScPeripherals : ModInitializer {
  internal val log = LoggerFactory.getLogger("ScPeripherals")!!

  internal const val modId = "sc-peripherals"
  internal fun ModId(value: String) = Identifier(modId, value)

  override fun onInitialize() {
    log.info("sc-peripherals initializing")

    // Initialize the default config file if it does not yet exist
    ScPeripheralsConfig.config.load()

    ScPeripheralsPrometheus.init()

    Registration.init()
  }
}
