package io.sc3.peripherals.client.gui

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
  private val tooltipKey: String? = null,
  private val direction: Direction = Direction.LEFT_TO_RIGHT
): DrawableHelper(), Drawable {
  var progress = 0

  enum class Direction {
    LEFT_TO_RIGHT, RIGHT_TO_LEFT,
    TOP_TO_BOTTOM, BOTTOM_TO_TOP
  }

  override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
    RenderSystem.setShader(GameRenderer::getPositionTexProgram)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, texture)

    val progress = progress.toDouble() / max.toDouble()
    val progressWidth = (progress * width).toInt()
    val progressHeight = (progress * height).toInt()
    when (direction) {
      Direction.LEFT_TO_RIGHT -> drawTexture(matrices, x, y, u, v, progressWidth, height)
      Direction.RIGHT_TO_LEFT -> drawTexture(matrices, x + width - progressWidth, y, u + width - progressWidth, v, progressWidth, height)
      Direction.TOP_TO_BOTTOM -> drawTexture(matrices, x, y, u, v, width, progressHeight)
      Direction.BOTTOM_TO_TOP -> drawTexture(matrices, x, y + height - progressHeight, u, v + height - progressHeight, width, progressHeight)
    }
  }

  fun isMouseOver(mouseX: Int, mouseY: Int) =
    mouseX in x..(x + width) && mouseY in y..(y + height)

  fun tooltip(mouseX: Int, mouseY: Int): Text? {
    if (!isMouseOver(mouseX, mouseY)) return null
    return tooltipKey?.let { Text.translatable(it, progress, max) }
  }
}
