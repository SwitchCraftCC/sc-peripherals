package io.sc3.peripherals.client.block

import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.client.gui.PosterPrinterScreen.Companion.tex
import io.sc3.peripherals.client.item.PosterRenderer
import io.sc3.peripherals.posters.PosterItem
import io.sc3.peripherals.posters.printer.OUTPUT_SLOT
import io.sc3.peripherals.posters.printer.PosterPrinterBlock
import io.sc3.peripherals.posters.printer.PosterPrinterBlockEntity
import io.sc3.peripherals.prints.printer.PrinterBlockEntity.Companion.maxInk
import net.minecraft.client.model.ModelData
import net.minecraft.client.model.ModelPart
import net.minecraft.client.model.ModelPartBuilder.create
import net.minecraft.client.model.ModelTransform.of
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f

private const val ROLLER_OFFSET_TICKS = 9 // 45 degrees

object PosterPrinterRenderer : BlockEntityRenderer<PosterPrinterBlockEntity> {
  private val texture = ModId("textures/entity/poster_printer.png")
  private val layer = RenderLayer.getEntityCutout(texture)

  override fun render(entity: PosterPrinterBlockEntity, tickDelta: Float, matrices: MatrixStack,
                      vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    renderPrinter(matrices, entity, tickDelta, vertexConsumers, light, overlay)
    renderInkOverlay(matrices, entity, vertexConsumers, light, overlay)
  }

  private fun renderPaperInTray(matrices: MatrixStack, entity: PosterPrinterBlockEntity, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    matrices.push()

    matrices.translate(0.5, 0.5, 0.5)
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
    matrices.translate(-0.5, -0.5, -0.5)
    matrices.translate(3.5f/16f, 6f/16f, 15f/16f - 0.001f)

    matrices.scale(1f/128f, 1f/128f, 1f/128f)
    matrices.scale(9/16f, 9/16f, 9/16f)
    PosterRenderer.drawBackground(matrices, vertexConsumers, light)

    matrices.pop()
  }

  private fun renderResult(matrices: MatrixStack, entity: PosterPrinterBlockEntity, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    val item = entity.getStack(OUTPUT_SLOT)
    if (item.isEmpty) return

    PosterItem.getPosterId(item)?.let { posterId ->
      matrices.push()

      matrices.translate(0.5, 0.5, 0.5)
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f))
      matrices.translate(-0.5, -0.5, -0.5)
      matrices.translate(3.5f / 16f, 1f / 16f, 14f / 16f - 0.001f)

      matrices.scale(1f / 128f, 1f / 128f, 1f / 128f)
      matrices.scale(9 / 16f, 9 / 16f, 9 / 16f)
      PosterRenderer.drawBackground(matrices, vertexConsumers, light)

      PosterItem.getPosterState(posterId, entity.world)?.let {
        PosterRenderer.draw(matrices, vertexConsumers, posterId, it, light)
      }

      matrices.pop()
    }
  }

  private fun renderAnimation(
    matrices: MatrixStack,
    entity: PosterPrinterBlockEntity,
    tickDelta: Float,
    vertexConsumers: VertexConsumerProvider,
    light: Int,
    overlay: Int
  ) {
    val output = entity.getStack(OUTPUT_SLOT)

    val posterId = entity.animatingPosterId ?: return
    val world = entity.world ?: return
    val animationTime = (world.time - entity.animationStartTime) + tickDelta
    val progress = (animationTime / entity.maxPrintProgress).coerceIn(0f, 1f)
    if (progress >= 1f) {
      val overshoot = world.time - (entity.animationStartTime + entity.maxPrintProgress)
      if (!output.isEmpty || overshoot > 20) { // give 20 tick buffer to receive inventory update
        entity.animatingPosterId = null
        return
      }
    }

    matrices.push()

    matrices.translate(0.5, 0.5, 0.5)
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f))
    matrices.translate(-0.5, -0.5, -0.5)
    matrices.translate(3.5f / 16f, (1f + 8f*(1f - progress)) / 16f, 14f / 16f - 0.002f)

    matrices.scale(1f / 128f, 1f / 128f, 1f / 128f)
    matrices.scale(9 / 16f, 9 / 16f, 9 / 16f)
    PosterRenderer.drawBackgroundCropped(
      matrices,
      vertexConsumers,
      light,
      0f,
      1f - progress,
      128f,
      128f * progress
    )

    PosterItem.getPosterState(posterId, entity.world)?.let {
      PosterRenderer.drawCropped(
        matrices,
        vertexConsumers,
        posterId,
        it,
        light,
        0f,
        1f - progress,
        128f,
        128f * progress
      )
    }

    matrices.pop()
  }

  private fun renderPrinter(
      matrices: MatrixStack,
      entity: PosterPrinterBlockEntity,
      tickDelta: Float,
      vertexConsumers: VertexConsumerProvider,
      light: Int,
      overlay: Int
  ) {
    val facing = entity.cachedState.get(PosterPrinterBlock.facing)
    val isPrinting = entity.cachedState.get(PosterPrinterBlock.printing)
    val animationTicks = entity.animationTicks

    matrices.push()

    matrices.translate(0.5, 0.5, 0.5)
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-facing.opposite.asRotation()))
    matrices.translate(-0.5, -0.5, -0.5)

    val animationProgress = (animationTicks + ROLLER_OFFSET_TICKS) + (if (isPrinting) tickDelta else 0.0f)
    roller.pitch = (animationProgress * 5) / 180f * -Math.PI.toFloat()

    val consumer = vertexConsumers.getBuffer(layer)
    base.render(matrices, consumer, light, overlay)
    roller.render(matrices, consumer, light, overlay)

    if (entity.cachedState.get(PosterPrinterBlock.hasPaper)) {
      renderPaperInTray(matrices, entity, vertexConsumers, light, overlay)
    }

    renderResult(matrices, entity, vertexConsumers, light, overlay)
    if (entity.animatingPosterId != null) {
      renderAnimation(matrices, entity, tickDelta, vertexConsumers, light, overlay)
    }

    matrices.pop()
  }

  private fun renderInkOverlay(matrices: MatrixStack, entity: PosterPrinterBlockEntity,
                               vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
    val facing = entity.cachedState.get(PosterPrinterBlock.facing)
    val ink = entity.ink

    matrices.push()

    matrices.translate(0.5, 0.5, 0.5)
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.opposite.asRotation()))
    matrices.translate(-0.5, -0.5, -0.5)

    matrices.translate(0.0, 0.0, -0.001)

    val inkWidth = (ink.toFloat() / maxInk.toFloat())
    drawQuad(matrices, vertexConsumers, 0.125f, 0.3125f, 0.4375f, 0, 240, inkWidth, light, overlay)

    matrices.pop()
  }

  private fun drawQuad(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider,
                       x0: Float, y0: Float, z: Float, u: Int, v: Int, width: Float, light: Int, overlay: Int) {
    val consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(tex))

    val x1 = x0 + (width * 0.75f)
    val y1 = y0 + 0.125f
    val u0 = u.toFloat() / 256.0f; val u1 = (u.toFloat() + (width * 48.0f)) / 256.0f
    val v0 = v.toFloat() / 256.0f; val v1 = (v.toFloat() + 8.0f) / 256.0f

    val entry = matrices.peek()
    val matrix = entry.positionMatrix
    val normalMatrix = entry.normalMatrix
    vertex(matrix, normalMatrix, consumer, x0, y0, z, u0, v1, light, overlay)
    vertex(matrix, normalMatrix, consumer, x1, y0, z, u1, v1, light, overlay)
    vertex(matrix, normalMatrix, consumer, x1, y1, z, u1, v0, light, overlay)
    vertex(matrix, normalMatrix, consumer, x0, y1, z, u0, v0, light, overlay)
  }

  private fun vertex(matrix: Matrix4f, normalMatrix: Matrix3f, vertexConsumer: VertexConsumer,
                     x: Float, y: Float, z: Float, u: Float, v: Float, light: Int, overlay: Int) {
    vertexConsumer
      .vertex(matrix, 1.0f - x, y, z)
      .color(255, 255, 255, 255)
      .texture(u, v)
      .overlay(overlay)
      .light(light)
      .normal(normalMatrix, 1.0f, 0.0f, 0.0f)
      .next()
  }

  private val modelData by lazy {
    val model = ModelData()
    val root = model.root

    root.addChild("root", create()
      .uv(0, 0).cuboid(0.0F, -2.0F, 4.0F, 16.0F, 2.0F, 12.0F)
      .uv(32, 31).cuboid(2.0F, -2.0F, 0.0F, 12.0F, 1.0F, 4.0F)
      .uv(0, 14).cuboid(0.0F, -10.0F, 9.0F, 16.0F, 8.0F, 7.0F)
      .uv(0, 4).cuboid(14.0F, -4.0F, 7.0F, 2.0F, 2.0F, 2.0F)
      .uv(0, 0).cuboid(0.0F, -4.0F, 7.0F, 2.0F, 2.0F, 2.0F)
      .uv(0, 29).cuboid(0.0F, -8.0F, 7.0F, 16.0F, 4.0F, 2.0F)
      .uv(0, 35).cuboid(2.0F, -16.0F, 15.0F, 12.0F, 6.0F, 1.0F),
      of(0.0F, 0.0F, 16.0F, 3.1415927F, 0.0F, 0.0F)
    )

    root.addChild("roller", create()
      .uv(34, 29).cuboid(10.0F, -0.5F, -0.5F, 12.0F, 1.0F, 1.0F),
      of(-8.0F, 3.0F, 7.7929F, -0.7854F, 0.0F, 0.0F)
    )

    TexturedModelData.of(model, 64, 64)
  }

  private val part: ModelPart = modelData.createModel()
  private val base: ModelPart = part.getChild("root")
  private val roller: ModelPart = part.getChild("roller")
}
