package pw.switchcraft.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import pw.switchcraft.peripherals.Registration.ModBlocks

class BlockModelProvider(out: FabricDataOutput) : FabricModelProvider(out) {
  override fun generateBlockStateModels(gen: BlockStateModelGenerator) {
    gen.registerSimpleCubeAll(ModBlocks.chamelium)
    gen.registerSimpleState(ModBlocks.print)
    gen.registerNorthDefaultHorizontalRotation(ModBlocks.printer)
  }

  override fun generateItemModels(gen: ItemModelGenerator) {
  }
}
