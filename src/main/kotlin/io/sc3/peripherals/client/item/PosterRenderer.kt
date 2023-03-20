package io.sc3.peripherals.client.item

import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.posters.PosterItem.Companion.getPosterId
import io.sc3.peripherals.posters.PosterItem.Companion.getPosterState
import io.sc3.peripherals.posters.PosterState
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.RotationAxis
import net.minecraft.world.World
import org.joml.Matrix4f

@Environment(EnvType.CLIENT)
object PosterRenderer : AutoCloseable {
  val POSTER_BACKGROUND_RES = ModId("textures/item/poster_background.png")
  val POSTER_BACKGROUND = RenderLayer.getText(POSTER_BACKGROUND_RES)

  private val textureManager get() = MinecraftClient.getInstance().textureManager

  private val posterTextures = mutableMapOf<String, PosterTexture>()
  fun updateTexture(id: String, state: PosterState) {
    getPosterTexture(id, state).setNeedsUpdate()
  }

  fun draw(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    id: String,
    state: PosterState,
    light: Int,
    doubleSided: Boolean = false
  ) {
    getPosterTexture(id, state).draw(matrices, vertexConsumers, light, doubleSided)
  }

  fun drawCropped(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    id: String,
    state: PosterState,
    light: Int,
    cropX: Float,
    cropY: Float,
    cropWidth: Float,
    cropHeight: Float
  ) {
    getPosterTexture(id, state).drawCropped(matrices, vertexConsumers, light, cropX, cropY, cropWidth, cropHeight)
  }

  private fun getPosterTexture(id: String, state: PosterState): PosterTexture
    = posterTextures
        .computeIfAbsent(id) { PosterTexture(id, state) }
        .also { it.setState(state) }

  fun clearStateTextures() {
    for (mapTexture in posterTextures.values) {
      mapTexture.close()
    }
    posterTextures.clear()
  }

  override fun close() {
    clearStateTextures()
  }

  private fun getLight(itemFrame: ItemFrameEntity, glowLight: Int, regularLight: Int): Int {
    return if (itemFrame.type === EntityType.GLOW_ITEM_FRAME) glowLight else regularLight
  }

  fun renderItemFrame(
    itemFrameEntity: ItemFrameEntity,
    itemStack: ItemStack,
    matrixStack: MatrixStack,
    vertexConsumerProvider: VertexConsumerProvider,
    light: Int
  ): Boolean {
    if (itemStack.isOf(ModItems.poster)) {
      val posterId = getPosterId(itemStack) ?: return false
      val posterState = getPosterState(posterId, itemFrameEntity.world) ?: return false

      // Ensure we can't do sideways rotation
      val j = itemFrameEntity.rotation % 4 * 2
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-itemFrameEntity.rotation.toFloat()) * 360.0f / 8.0f))
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(j.toFloat() * 360.0f / 8.0f))

      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f))
      matrixStack.scale(1f/128f, 1f/128f, 1f/128f)
      matrixStack.translate(-64.0f, -64.0f, 0.0f)

      matrixStack.translate(0.0f, 0.0f, -1.0f)

      val evaluatedLight = getLight(itemFrameEntity, 15728850, light)

      draw(
        matrixStack,
        vertexConsumerProvider,
        posterId,
        posterState,
        evaluatedLight
      )

      return true
    }

    return false
  }

  fun drawBackground(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, swingProgress: Int) {
    val vertexConsumer = vertexConsumers.getBuffer(POSTER_BACKGROUND)
    val matrix4f = matrices.peek().positionMatrix
    vertexConsumer.vertex(matrix4f, -7.0f, 135.0f, 0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(swingProgress).next()
    vertexConsumer.vertex(matrix4f, 135.0f, 135.0f, 0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(swingProgress).next()
    vertexConsumer.vertex(matrix4f, 135.0f, -7.0f, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(swingProgress).next()
    vertexConsumer.vertex(matrix4f, -7.0f, -7.0f, 0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(swingProgress).next()
  }

  fun drawBackgroundCropped(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    swingProgress: Int,
    x: Float, y: Float, width: Float, height: Float
  ) {
    val vertexConsumer = vertexConsumers.getBuffer(POSTER_BACKGROUND)
    val matrix4f = matrices.peek().positionMatrix
    drawCrop(vertexConsumer, matrix4f, swingProgress, 0f, x-7, y-7, width+14, height+14, 128f+14f, 7f, 7f)
  }

  @Environment(EnvType.CLIENT)
  internal class PosterTexture(id: String, private var state: PosterState) :
    AutoCloseable {
    private val texture: NativeImageBackedTexture = NativeImageBackedTexture(128, 128, true)
    private val renderLayer: RenderLayer
    private var needsUpdate = true

    init {
      val identifier = textureManager.registerDynamicTexture("poster/$id", texture)
      renderLayer = RenderLayer.getText(identifier)
    }

    fun setState(state: PosterState) {
      val bl = this.state !== state
      this.state = state
      needsUpdate = needsUpdate or bl
    }

    fun setNeedsUpdate() {
      needsUpdate = true
    }

    private fun getRenderColor(colorIndex: Int): Int {
      return if (colorIndex == 0) {
        0
      } else {
        val color = state.palette.getOrNull(colorIndex) ?: return 0
        val r: Int = color shr 16 and 0xFF
        val g: Int = color shr 8 and 0xFF
        val b: Int = color and 0xFF
        -0x1000000 or (b shl 16) or (g shl 8) or r
      }
    }

    private fun updateTexture() {
      val image = texture.image ?: return
      for (y in 0..127) {
        for (x in 0..127) {
          val idx = x + y * 128
          image.setColor(x, y, getRenderColor(state.colors[idx].toInt()))
        }
      }

      texture.upload()
    }

    fun draw(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, doubleSided: Boolean) {
      if (needsUpdate) {
        this.updateTexture()
        needsUpdate = false
      }

      val matrix4f = matrices.peek().positionMatrix
      val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
      vertexConsumer.vertex(matrix4f, 0.0f, 128.0f, -0.01f).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(light).next()
      vertexConsumer.vertex(matrix4f, 128.0f, 128.0f, -0.01f).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(light).next()
      vertexConsumer.vertex(matrix4f, 128.0f, 0.0f, -0.01f).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(light).next()
      vertexConsumer.vertex(matrix4f, 0.0f, 0.0f, -0.01f).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(light).next()

      if (doubleSided) {
        vertexConsumer.vertex(matrix4f, 0.0f, 0.0f, -0.01f).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(light).next()
        vertexConsumer.vertex(matrix4f, 128.0f, 0.0f, -0.01f).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(light).next()
        vertexConsumer.vertex(matrix4f, 128.0f, 128.0f, -0.01f).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(light).next()
        vertexConsumer.vertex(matrix4f, 0.0f, 128.0f, -0.01f).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(light).next()
      }
    }

    fun drawCropped(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, x: Float, y: Float, width: Float, height: Float) {
      if (needsUpdate) {
        this.updateTexture()
        needsUpdate = false
      }

      val matrix4f = matrices.peek().positionMatrix
      val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
      drawCrop(vertexConsumer, matrix4f, light, -0.01f, x, y, width, height,128f)
    }

    override fun close() {
      texture.close()
    }
  }

  private fun drawCrop(
      vertexConsumer: VertexConsumer,
      matrix4f: Matrix4f?,
      light: Int,
      z: Float,
      x: Float,
      y: Float,
      width: Float,
      height: Float,
      size: Float,
      uvOffsetX: Float = 0f,
      uvOffsetY: Float = 0f
  ) {
    vertexConsumer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture((x + uvOffsetX) / size, (y + uvOffsetY + height) / size).light(light).next()
    vertexConsumer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture((x + uvOffsetX + width) / size, (y + uvOffsetY + height) / size).light(light).next()
    vertexConsumer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture((x + uvOffsetX + width) / size, (y + uvOffsetY) / size).light(light).next()
    vertexConsumer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture((x + uvOffsetX) / size, (y + uvOffsetY) / size).light(light).next()
  }

  @JvmStatic
  fun renderFirstPersonMap(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, swingProgress: Int, stack: ItemStack, world: World?) {
    drawBackground(matrices, vertexConsumers, swingProgress)

    val posterId = getPosterId(stack) ?: return
    val posterState = getPosterState(posterId, world)
    if (posterState != null) {
      draw(matrices, vertexConsumers, posterId, posterState, swingProgress)
    }
  }
}
