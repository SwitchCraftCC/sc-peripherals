package io.sc3.peripherals.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.text.Text.of

class ModMenu: ModMenuApi {
  override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent ->
    val builder = ConfigBuilder.create().setParentScreen(parent)
      .setTitle(of("SwitchCraft Peripherals"))
      .setSavingRunnable {
        ScPeripheralsClientConfig.spec.correct(ScPeripheralsClientConfig.config)
        ScPeripheralsClientConfig.config.save()
      }

    val client = builder.getOrCreateCategory(of("Client"))
    client.addEntry(builder.entryBuilder()
      .startIntSlider(
        of("Max Poster Requests Per Tick"),
        ScPeripheralsClientConfig.config["maxPosterRequestsPerTick"], 1, 50
      )
      .setDefaultValue(20)
      .setSaveConsumer { ScPeripheralsClientConfig.config.set("maxPosterRequestsPerTick", it) }
      .build()
    )

    builder.build()
  }
}
