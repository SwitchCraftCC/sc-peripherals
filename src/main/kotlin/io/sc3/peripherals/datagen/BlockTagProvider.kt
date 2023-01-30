package io.sc3.peripherals.datagen

import io.sc3.peripherals.Registration.ModBlocks
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.block.Block
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import java.util.concurrent.CompletableFuture

class BlockTagProvider(
  out: FabricDataOutput,
  future: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider<Block>(out, RegistryKeys.BLOCK, future) {
  override fun configure(arg: RegistryWrapper.WrapperLookup) {
    getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
      .add(ModBlocks.print, ModBlocks.printer, ModBlocks.posterPrinter)
  }
}
