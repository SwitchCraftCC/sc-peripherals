package io.sc3.peripherals

import dan200.computercraft.api.peripheral.PeripheralLookup
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
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
import net.minecraft.registry.Registries.*
import net.minecraft.registry.Registry.register
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.math.BlockPos
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.block.ChameliumBlock
import io.sc3.peripherals.datagen.recipes.handlers.RecipeHandlers
import io.sc3.peripherals.item.ChameliumItem
import io.sc3.peripherals.item.EmptyInkCartridgeItem
import io.sc3.peripherals.item.InkCartridgeItem
import io.sc3.peripherals.item.TextureAnalyzerItem
import io.sc3.peripherals.prints.PrintBlock
import io.sc3.peripherals.prints.PrintBlockEntity
import io.sc3.peripherals.prints.PrintItem
import io.sc3.peripherals.prints.printer.PrinterBlock
import io.sc3.peripherals.prints.printer.PrinterBlockEntity
import io.sc3.peripherals.prints.printer.PrinterScreenHandler

object Registration {
  private val items = mutableListOf<Item>()

  internal fun init() {
    // Similar to how CC behaves - touch each static class to force the static initializers to run.
    listOf(ModBlocks.printer, ModItems.printer, ModBlockEntities.printer, ModScreens.printer)

    RecipeHandlers.registerSerializers()

    PeripheralLookup.get().registerForBlockEntity({ be, _ -> be.peripheral }, ModBlockEntities.printer)
  }

  object ModBlocks {
    val printer = rBlock("printer", PrinterBlock(settings()))
    val print = rBlock("print", PrintBlock(settings()
      .nonOpaque()
      .dynamicBounds()
      .luminance { it.get(PrintBlock.luminance) }))

    val chamelium = rBlock("chamelium", ChameliumBlock(settings()))

    private fun <T : Block> rBlock(name: String, value: T): T =
      register(BLOCK, ModId(name), value)
    private fun settings() = AbstractBlock.Settings.of(STONE).strength(2.0f).nonOpaque()
  }

  object ModItems {
    val printer = ofBlock(ModBlocks.printer, ::BlockItem)
    val print = rItem("print", PrintItem(Item.Settings()), addItem = false)

    val chamelium = rItem("chamelium", ChameliumItem(settings()))
    val inkCartridge = rItem("ink_cartridge", InkCartridgeItem(settings().maxCount(1)))
    val emptyInkCartridge = rItem("empty_ink_cartridge", EmptyInkCartridgeItem(settings().maxCount(1)))
    val textureAnalyzer = rItem("texture_analyzer", TextureAnalyzerItem(settings().maxCount(1)))

    init {
      FabricItemGroup.builder(ModId("main"))
        .icon { ItemStack(printer) }
        .entries { _, entries, _ -> items.forEach(entries::add) }
        .build()
    }

    private fun <T : Item> rItem(name: String, value: T, addItem: Boolean = true): T =
      register(ITEM, ModId(name), value).also { items.takeIf { addItem }?.add(it) }
    private fun <B : Block, I : Item> ofBlock(parent: B, supplier: (B, Item.Settings) -> I): I =
      register(ITEM, BLOCK.getId(parent), supplier(parent, settings())).also { items.add(it) }
    private fun settings() = Item.Settings()
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
