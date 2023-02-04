package io.sc3.peripherals.datagen

import io.sc3.peripherals.Registration.ModBlocks
import io.sc3.peripherals.prints.PrintBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.entry.DynamicEntry
import net.minecraft.loot.provider.number.ConstantLootNumberProvider

class BlockLootTableProvider(out: FabricDataOutput) : FabricBlockLootTableProvider(out) {
  override fun generate() {
    addDrop(ModBlocks.printer)
    addDrop(
      ModBlocks.print, LootTable.builder().pool(
        addSurvivesExplosionCondition(
          ModBlocks.print,
          LootPool.builder()
            .rolls(ConstantLootNumberProvider.create(1.0F))
            .with(DynamicEntry.builder(PrintBlock.dropId))
        )
      )
    )

    addDrop(ModBlocks.posterPrinter)
  }
}
