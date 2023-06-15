package io.sc3.peripherals.client.gui

import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.ScPeripherals.modId
import io.sc3.peripherals.prints.printer.PrinterBlockEntity.Companion.maxChamelium
import io.sc3.peripherals.prints.printer.PrinterBlockEntity.Companion.maxInk
import io.sc3.peripherals.prints.printer.PrinterScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

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

  override fun drawBackground(ctx: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
    // Background
    ctx.drawTexture(tex, x, y, 0, 0, backgroundWidth, backgroundHeight)

    // Print progress
    val maxProgress = handler.maxPrintProgress.toDouble().coerceAtLeast(1.0) // Avoid div0 if the server sets this to 0
    val progress = handler.printProgress.toDouble() / maxProgress * progressWidth
    ctx.drawTexture(tex, x + 106, y + 24, 0, 166, progress.toInt(), 38)
  }

  override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    renderBackground(ctx)

    chameliumBar.progress = handler.chamelium
    inkBar.progress = handler.ink

    super.render(ctx, mouseX, mouseY, delta)
    drawMouseoverTooltip(ctx, mouseX, mouseY)

    bars.forEach { bar -> bar.tooltip(mouseX, mouseY)?.let {
      ctx.drawTooltip(textRenderer, it, mouseX, mouseY )}
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
