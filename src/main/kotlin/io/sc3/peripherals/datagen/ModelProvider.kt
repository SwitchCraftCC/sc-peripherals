package io.sc3.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.data.client.Models.GENERATED
import io.sc3.peripherals.Registration.ModBlocks
import io.sc3.peripherals.Registration.ModItems

class ModelProvider(out: FabricDataOutput) : FabricModelProvider(out) {
  override fun generateBlockStateModels(gen: BlockStateModelGenerator) {
    gen.registerSimpleCubeAll(ModBlocks.chamelium)
    gen.registerSimpleState(ModBlocks.print)
    gen.registerNorthDefaultHorizontalRotation(ModBlocks.printer)
    gen.registerNorthDefaultHorizontalRotation(ModBlocks.posterPrinter)
  }

  override fun generateItemModels(gen: ItemModelGenerator) {
    gen.register(ModItems.chamelium, GENERATED)
    gen.register(ModItems.inkCartridge, GENERATED)
    gen.register(ModItems.emptyInkCartridge, GENERATED)
    gen.register(ModItems.textureAnalyzer, GENERATED)
    gen.register(ModItems.poster, GENERATED)
  }
}
