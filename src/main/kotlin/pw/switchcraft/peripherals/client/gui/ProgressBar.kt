package pw.switchcraft.peripherals.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ProgressBar(
  private val texture: Identifier,
  private val x: Int, private val y: Int,
  private val u: Int, private val v: Int,
  private val width: Int = 62, private val height: Int = 12,
  private val max: Int = 100,
  private val tooltipKey: String? = null
): DrawableHelper(), Drawable {
  var progress = 0

  override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, texture)

    val progressWidth = ((progress.toDouble() / max.toDouble()) * width).toInt()
    drawTexture(matrices, x, y, u, v, progressWidth, height)
  }

  fun isMouseOver(mouseX: Int, mouseY: Int) =
    mouseX in x..(x + width) && mouseY in y..(y + height)

  fun tooltip(mouseX: Int, mouseY: Int): Text? {
    if (!isMouseOver(mouseX, mouseY)) return null
    return tooltipKey?.let { Text.translatable(it, progress, max) }
  }
}
