package pw.switchcraft.peripherals.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.data.client.Models.GENERATED
import pw.switchcraft.peripherals.Registration.ModItems

class ItemModelProvider(out: FabricDataOutput) : FabricModelProvider(out) {
  override fun generateBlockStateModels(gen: BlockStateModelGenerator) {
  }

  override fun generateItemModels(gen: ItemModelGenerator) {
    gen.register(ModItems.chamelium, GENERATED)
    gen.register(ModItems.inkCartridge, GENERATED)
    gen.register(ModItems.emptyInkCartridge, GENERATED)
    gen.register(ModItems.textureAnalyzer, GENERATED)
  }
}
