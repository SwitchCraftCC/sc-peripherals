package io.sc3.peripherals.client.gui

import io.sc3.peripherals.client.gui.ProgressBar.Direction.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ProgressBar(
  private val tex: Identifier,
  private val x: Int, private val y: Int,
  private val u: Int, private val v: Int,
  private val width: Int = 62, private val height: Int = 12,
  private val max: Int = 100,
  private val tooltipKey: String? = null,
  private val direction: Direction = LEFT_TO_RIGHT
) : Drawable {
  var progress = 0

  enum class Direction {
    LEFT_TO_RIGHT, RIGHT_TO_LEFT,
    TOP_TO_BOTTOM, BOTTOM_TO_TOP
  }

  override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    val progress = progress.toDouble() / max.toDouble()
    val progressWidth = (progress * width).toInt()
    val progressHeight = (progress * height).toInt()
    when (direction) {
      LEFT_TO_RIGHT -> ctx.drawTexture(tex, x, y, u, v, progressWidth, height)
      RIGHT_TO_LEFT -> ctx.drawTexture(tex, x + width - progressWidth, y, u + width - progressWidth, v, progressWidth, height)
      TOP_TO_BOTTOM -> ctx.drawTexture(tex, x, y, u, v, width, progressHeight)
      BOTTOM_TO_TOP -> ctx.drawTexture(tex, x, y + height - progressHeight, u, v + height - progressHeight, width, progressHeight)
    }
  }

  private fun isMouseOver(mouseX: Int, mouseY: Int) =
    mouseX in x..(x + width) && mouseY in y..(y + height)

  fun tooltip(mouseX: Int, mouseY: Int): Text? {
    if (!isMouseOver(mouseX, mouseY)) return null
    return tooltipKey?.let { Text.translatable(it, progress, max) }
  }
}
