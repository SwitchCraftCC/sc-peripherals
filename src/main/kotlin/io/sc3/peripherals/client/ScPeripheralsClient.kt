package io.sc3.peripherals.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelResourceProvider
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.client.render.RenderLayer
import org.slf4j.LoggerFactory
import io.sc3.library.networking.registerClientReceiver
import io.sc3.peripherals.Registration
import io.sc3.peripherals.client.block.PrintBakedModel
import io.sc3.peripherals.client.block.PrintUnbakedModel
import io.sc3.peripherals.client.block.PrinterRenderer
import io.sc3.peripherals.client.gui.PrinterScreen
import io.sc3.peripherals.prints.PrintBlock
import io.sc3.peripherals.prints.PrintItem
import io.sc3.peripherals.prints.printer.PrinterDataPacket
import io.sc3.peripherals.prints.printer.PrinterInkPacket
import io.sc3.peripherals.util.ScreenHandlerPropertyUpdateIntS2CPacket

object ScPeripheralsClient : ClientModInitializer {
  val log = LoggerFactory.getLogger("ScPeripherals/ScPeripheralsClient")!!

  override fun onInitializeClient() {
    log.info("sc-peripherals client initializing")

    BlockEntityRendererRegistry.register(Registration.ModBlockEntities.printer) { PrinterRenderer }
    HandledScreens.register(Registration.ModScreens.printer, ::PrinterScreen)

    // Allow transparent textures to work in 3D prints
    BlockRenderLayerMap.INSTANCE.putBlock(Registration.ModBlocks.print, RenderLayer.getTranslucent())

    ModelLoadingRegistry.INSTANCE.registerResourceProvider { ModelResourceProvider { id, _ -> when(id) {
      PrintBlock.id, PrintItem.id -> PrintUnbakedModel()
      else -> null
    }}}

    registerClientReceiver(PrinterInkPacket.id, PrinterInkPacket::fromBytes)
    registerClientReceiver(PrinterDataPacket.id, PrinterDataPacket::fromBytes)

    // Vanilla ScreenHandlerPropertyUpdateS2CPacket sends shorts instead of ints over the wire
    ScreenHandlerPropertyUpdateIntS2CPacket.registerReceiver()

    // Clear 3D print cache on resource reload
    PrintBakedModel.init()
  }
}
