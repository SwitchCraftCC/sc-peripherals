package io.sc3.peripherals.datagen

import io.sc3.peripherals.Registration.ModBlocks
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider

class BlockLootTableProvider(out: FabricDataOutput) : FabricBlockLootTableProvider(out) {
  override fun generate() {
    addDrop(ModBlocks.printer)
  }
}
