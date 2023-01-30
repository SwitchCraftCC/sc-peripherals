package io.sc3.peripherals.client.item

import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.posters.PosterItem
import io.sc3.peripherals.posters.PosterState
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.MapColor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4f
import java.util.function.BiFunction

@Environment(EnvType.CLIENT)
object PosterRenderer : AutoCloseable {
  val POSTER_BACKGROUND_RES = ModId("textures/item/poster_background.png")
  val POSTER_BACKGROUND = RenderLayer.getText(POSTER_BACKGROUND_RES)

//  val MAP_ICONS_RENDER_LAYER = RenderLayer.getText(MAP_ICONS_TEXTURE)
//  private const val DEFAULT_IMAGE_WIDTH = 128
//  private const val DEFAULT_IMAGE_HEIGHT = 128

  private val textureManager get() = MinecraftClient.getInstance().textureManager

  private val posterTextures = mutableMapOf<String, PosterTexture>() // : Int2ObjectMap<PosterTexture> = Int2ObjectOpenHashMap()
  fun updateTexture(id: String, state: PosterState) {
    getPosterTexture(id, state).setNeedsUpdate()
  }

  fun draw(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    id: String,
    state: PosterState,
    light: Int
  ) {
    getPosterTexture(id, state).draw(matrices, vertexConsumers, light)
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

  private fun getPosterTexture(id: String, state: PosterState): PosterTexture {
    return posterTextures.compute(id,
      BiFunction<String, PosterTexture?, PosterTexture?> { id2: String, texture: PosterTexture? ->
        if (texture == null) {
          return@BiFunction PosterTexture(id2, state)
        } else {
          texture.setState(state)
          return@BiFunction texture
        }
      })!!
  }

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
      val posterId = PosterItem.getPosterId(itemStack) ?: return false
      val posterState = PosterItem.getPosterState(posterId, itemFrameEntity.world) ?: return false

      // Ensure we can't do sideways rotation
      val j = itemFrameEntity.rotation % 4 * 2
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-itemFrameEntity.rotation.toFloat()) * 360.0f / 8.0f))
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(j.toFloat() * 360.0f / 8.0f))

      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f))
      val h = 0.0078125f
      matrixStack.scale(0.0078125f, 0.0078125f, 0.0078125f)
      matrixStack.translate(-64.0f, -64.0f, 0.0f)

      matrixStack.translate(0.0f, 0.0f, -1.0f)

      val k: Int = getLight(itemFrameEntity, 15728850, light)

      draw(
        matrixStack,
        vertexConsumerProvider,
        posterId,
        posterState,
        k
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

    fun getRenderColor(colorIndex: Int, brightness: MapColor.Brightness): Int {
      return if (colorIndex === 0) {
        0
      } else {
        val color = state.palette.getOrNull(colorIndex) ?: return 0
        val i = brightness.brightness
        val j: Int = (color shr 16 and 0xFF) * i / 255
        val k: Int = (color shr 8 and 0xFF) * i / 255
        val l: Int = (color and 0xFF) * i / 255
        -0x1000000 or (l shl 16) or (k shl 8) or j
      }
    }

    private fun updateTexture() {
      for (i in 0..127) {
        for (j in 0..127) {
          val k = j + i * 128
          texture.image!!.setColor(j, i, getRenderColor(state.colors[k].toInt(), MapColor.Brightness.NORMAL))
        }
      }
      texture.upload()
    }

    fun draw(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int) {
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
}
