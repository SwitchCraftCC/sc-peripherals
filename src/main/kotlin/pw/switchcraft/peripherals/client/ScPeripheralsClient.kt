package pw.switchcraft.peripherals.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelResourceProvider
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.client.render.RenderLayer
import org.slf4j.LoggerFactory
import pw.switchcraft.peripherals.Registration
import pw.switchcraft.peripherals.client.block.PrintUnbakedModel
import pw.switchcraft.peripherals.client.block.PrinterRenderer
import pw.switchcraft.peripherals.client.gui.PrinterScreen
import pw.switchcraft.peripherals.prints.PrintBlock
import pw.switchcraft.peripherals.prints.PrintItem
import pw.switchcraft.peripherals.prints.printer.PrinterInkPacket
import pw.switchcraft.peripherals.prints.printer.PrinterDataPacket
import pw.switchcraft.peripherals.util.registerClientReceiver

object ScPeripheralsClient : ClientModInitializer {
  val log = LoggerFactory.getLogger("ScPeripherals/ScPeripheralsClient")!!

  override fun onInitializeClient() {
    log.info("sc-peripherals client initializing")

    BlockEntityRendererRegistry.register(Registration.ModBlockEntities.printer) { PrinterRenderer }
    HandledScreens.register(Registration.ModScreens.printer, ::PrinterScreen)

    // Allow transparent textures to work in 3D prints
    BlockRenderLayerMap.INSTANCE.putBlock(Registration.ModBlocks.print, RenderLayer.getCutoutMipped())

    ModelLoadingRegistry.INSTANCE.registerResourceProvider { ModelResourceProvider { id, _ -> when(id) {
      PrintBlock.id, PrintItem.id -> PrintUnbakedModel()
      else -> null
    }}}

    registerClientReceiver(PrinterInkPacket.id, PrinterInkPacket::fromBytes)
    registerClientReceiver(PrinterDataPacket.id, PrinterDataPacket::fromBytes)
  }
}
