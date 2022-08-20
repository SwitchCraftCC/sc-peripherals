package pw.switchcraft.peripherals

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Material.STONE
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry.*
import pw.switchcraft.peripherals.ScPeripherals.ModId
import pw.switchcraft.peripherals.block.ChameliumBlock
import pw.switchcraft.peripherals.item.ChameliumItem
import pw.switchcraft.peripherals.item.EmptyInkCartridgeItem
import pw.switchcraft.peripherals.item.InkCartridgeItem
import pw.switchcraft.peripherals.item.TextureAnalyzerItem
import pw.switchcraft.peripherals.prints.PrintBlock
import pw.switchcraft.peripherals.prints.PrintBlockEntity
import pw.switchcraft.peripherals.prints.PrintItem
import pw.switchcraft.peripherals.prints.PrintRecipe
import pw.switchcraft.peripherals.prints.printer.PrinterBlock
import pw.switchcraft.peripherals.prints.printer.PrinterBlockEntity
import pw.switchcraft.peripherals.prints.printer.PrinterRecipe
import pw.switchcraft.peripherals.prints.printer.PrinterScreenHandler

object Registration {
  internal fun init() {
    // Similar to how CC behaves - touch each static class to force the static initializers to run.
    listOf(ModBlocks.printer, ModItems.printer, ModBlockEntities.printer, ModScreens.printer)

    register(RECIPE_SERIALIZER, ModId("printer"), PrinterRecipe.recipeSerializer)
    register(RECIPE_SERIALIZER, ModId("print"), PrintRecipe.recipeSerializer)
  }

  object ModBlocks {
    val printer = register("printer", PrinterBlock(settings()))
    val print = register("print", PrintBlock(settings()
      .nonOpaque()
      .dynamicBounds()
      .luminance { it.get(PrintBlock.luminance) }))

    val chamelium = register("chamelium", ChameliumBlock(settings()))

    private fun <T : Block> register(name: String, value: T): T =
      register(BLOCK, ModId(name), value)
    private fun settings() = AbstractBlock.Settings.of(STONE).strength(2.0f).nonOpaque()
  }

  object ModItems {
    val itemGroup = FabricItemGroupBuilder.build(ModId("main")) { ItemStack(printer) }

    val printer = ofBlock(ModBlocks.printer, ::BlockItem)
    val print = register("print", PrintItem(Item.Settings())) // no group

    val chamelium = register("chamelium", ChameliumItem(settings()))
    val inkCartridge = register("ink_cartridge", InkCartridgeItem(settings().maxCount(1)))
    val emptyInkCartridge = register("empty_ink_cartridge", EmptyInkCartridgeItem(settings().maxCount(1)))
    val textureAnalyzer = register("texture_analyzer", TextureAnalyzerItem(settings().maxCount(1)))

    private fun <T : Item> register(name: String, value: T): T =
      register(ITEM, ModId(name), value)
    private fun <B : Block, I : Item> ofBlock(parent: B, supplier: (B, Item.Settings) -> I): I =
      register(ITEM, BLOCK.getId(parent), supplier(parent, settings()))
    private fun settings() = Item.Settings().group(itemGroup)
  }

  object ModBlockEntities {
    val printer: BlockEntityType<PrinterBlockEntity> = ofBlock(ModBlocks.printer, "printer", ::PrinterBlockEntity)
    val print: BlockEntityType<PrintBlockEntity> = ofBlock(ModBlocks.print, "print", ::PrintBlockEntity)

    private fun <T : BlockEntity> ofBlock(block: Block, name: String,
                                          factory: (BlockPos, BlockState) -> T): BlockEntityType<T> {
      val blockEntityType = FabricBlockEntityTypeBuilder.create(factory, block).build()
      return register(BLOCK_ENTITY_TYPE, ModId(name), blockEntityType)
    }
  }

  object ModScreens {
    val printer = register(SCREEN_HANDLER, ModId("printer"), ScreenHandlerType(::PrinterScreenHandler))
  }
}
