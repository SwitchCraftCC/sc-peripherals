package pw.switchcraft.peripherals.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import pw.switchcraft.peripherals.ScPeripherals.ModId
import pw.switchcraft.peripherals.ScPeripherals.modId
import pw.switchcraft.peripherals.prints.printer.PrinterBlockEntity.Companion.maxChamelium
import pw.switchcraft.peripherals.prints.printer.PrinterBlockEntity.Companion.maxInk
import pw.switchcraft.peripherals.prints.printer.PrinterScreenHandler

class PrinterScreen(
  handler: PrinterScreenHandler,
  playerInv: PlayerInventory,
  title: Text
) : HandledScreen<PrinterScreenHandler>(handler, playerInv, title) {
  private lateinit var chameliumBar: ProgressBar
  private lateinit var inkBar: ProgressBar
  private lateinit var bars: List<ProgressBar>

  override fun init() {
    super.init()

    chameliumBar = addDrawable(ProgressBar(tex, x + 39, y + 19, chameliumU, chameliumV, max = maxChamelium,
      tooltipKey = "gui.$modId.printer.chamelium"))
    inkBar = addDrawable(ProgressBar(tex, x + 39, y + 55, inkU, inkV, max = maxInk,
      tooltipKey = "gui.$modId.printer.ink"))
    bars = listOf(chameliumBar, inkBar)
  }

  override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, tex)

    // Background
    drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)

    // Print progress
    val maxProgress = handler.maxPrintProgress.toDouble().coerceAtLeast(1.0) // Avoid div0 if the server sets this to 0
    val progress = handler.printProgress.toDouble() / maxProgress * progressWidth
    drawTexture(matrices, x + 106, y + 24, 0, 166, progress.toInt(), 38)
  }

  override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
    renderBackground(matrices)

    chameliumBar.progress = handler.chamelium
    inkBar.progress = handler.ink

    super.render(matrices, mouseX, mouseY, delta)
    drawMouseoverTooltip(matrices, mouseX, mouseY)

    bars.forEach { bar -> bar.tooltip(mouseX, mouseY)?.let {
      renderTooltip(matrices, it, mouseX, mouseY )}
    }
  }

  companion object {
    val tex = ModId("textures/gui/container/printer.png")

    const val chameliumU = 1
    const val chameliumV = 205
    const val inkU = 1
    const val inkV = 219
    const val progressWidth = 24
  }
}
