package io.sc3.peripherals.client.block

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.model.json.ModelTransformation.Mode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f
import io.sc3.peripherals.client.gui.PrinterScreen.Companion.tex
import io.sc3.peripherals.prints.printer.PrinterBlock
import io.sc3.peripherals.prints.printer.PrinterBlockEntity
import io.sc3.peripherals.prints.printer.PrinterBlockEntity.Companion.maxChamelium
import io.sc3.peripherals.prints.printer.PrinterBlockEntity.Companion.maxInk

object PrinterRenderer : BlockEntityRenderer<PrinterBlockEntity> {
  private val mc by lazy { MinecraftClient.getInstance() }
  private val itemRenderer by mc::itemRenderer

  override fun render(entity: PrinterBlockEntity, tickDelta: Float, matrices: MatrixStack,
                      vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    renderPrint(matrices, entity, vertexConsumers, light, overlay)
    renderInkOverlay(matrices, entity, vertexConsumers, light, overlay)
  }

  private fun renderPrint(matrices: MatrixStack, printer: PrinterBlockEntity, vertexConsumers: VertexConsumerProvider,
                          light: Int, overlay: Int) {
    val stack = printer.previewStack
    if (stack.isEmpty) return

    matrices.push()

    matrices.translate(0.5, 0.7, 0.5)

    val animationAngle = (System.currentTimeMillis() % 20000) / 20000.0f * 360.0f
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animationAngle))
    matrices.scale(0.5f, 0.5f, 0.5f)

    val model = itemRenderer.getModel(stack, printer.world, null, 0)
    itemRenderer.renderItem(stack, Mode.FIXED, false, matrices, vertexConsumers, light, overlay, model)

    matrices.pop()
  }

  private fun renderInkOverlay(matrices: MatrixStack, entity: PrinterBlockEntity,
                               vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    val facing = entity.cachedState.get(PrinterBlock.facing)
    val chamelium = entity.chamelium
    val ink = entity.ink

    matrices.push()

    matrices.translate(0.5, 0.5, 0.5)
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.opposite.asRotation()))
    matrices.translate(-0.5, -0.5, -0.5)

    matrices.translate(0.0, 0.0, -0.001)

    val chameliumWidth = (chamelium.toFloat() / maxChamelium.toFloat())
    drawQuad(matrices, vertexConsumers, 0.1875f, 0.1875f, 0, 232, chameliumWidth, light, overlay)

    val inkWidth = (ink.toFloat() / maxInk.toFloat())
    drawQuad(matrices, vertexConsumers, 0.5625f, 0.1875f, 0, 240, inkWidth, light, overlay)

    matrices.pop()
  }

  private fun drawQuad(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider,
                       x0: Float, y0: Float, u: Int, v: Int, width: Float, light: Int, overlay: Int) {
    val consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(tex))

    val x1 = x0 + (width * 0.25f)
    val y1 = y0 + 0.125f
    val u0 = u.toFloat() / 256.0f; val u1 = (u.toFloat() + (width * 16.0f)) / 256.0f
    val v0 = v.toFloat() / 256.0f; val v1 = (v.toFloat() + 8.0f) / 256.0f

    val entry = matrices.peek()
    val matrix = entry.positionMatrix
    val normalMatrix = entry.normalMatrix
    vertex(matrix, normalMatrix, consumer, x0, y0, u0, v1, light, overlay)
    vertex(matrix, normalMatrix, consumer, x1, y0, u1, v1, light, overlay)
    vertex(matrix, normalMatrix, consumer, x1, y1, u1, v0, light, overlay)
    vertex(matrix, normalMatrix, consumer, x0, y1, u0, v0, light, overlay)
  }

  private fun vertex(matrix: Matrix4f, normalMatrix: Matrix3f, vertexConsumer: VertexConsumer,
                     x: Float, y: Float, u: Float, v: Float, light: Int, overlay: Int) {
    vertexConsumer
      .vertex(matrix, 1.0f - x, y, 0.0f)
      .color(255, 255, 255, 255)
      .texture(u, v)
      .overlay(overlay)
      .light(light)
      .normal(normalMatrix, 1.0f, 0.0f, 0.0f)
      .next()
  }
}
