package io.sc3.peripherals.client.block

import io.sc3.library.ext.faces
import io.sc3.library.ext.rotateTowards
import io.sc3.peripherals.Registration.ModBlockEntities
import io.sc3.peripherals.ScPeripherals.ModId
import io.sc3.peripherals.prints.*
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView.BAKE_LOCK_UV
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.item.ItemStack
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE
import net.minecraft.text.Text.literal
import net.minecraft.util.Formatting.BOLD
import net.minecraft.util.Formatting.YELLOW
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import org.cache2k.Cache2kBuilder
import java.util.function.Supplier

class PrintBakedModel(
  private val sprite: Sprite
) : BakedModel, FabricBakedModel {
  private val mc by lazy { MinecraftClient.getInstance() }
  private val missingModel by lazy { mc.bakedModelManager.missingModel }

  override fun isVanillaAdapter() = false

  override fun emitBlockQuads(blockView: BlockRenderView, state: BlockState, pos: BlockPos,
                              randomSupplier: Supplier<Random>, ctx: RenderContext) {
    val be = blockView.getBlockEntity(pos, ModBlockEntities.print).orElse(null) ?: return // TODO: Return missingModel?
    val shapes = be.shapes // Get the shapes for the current state (on/off)
    val shapesFacing = ShapesFacing(shapes, be.facing)

    val mesh = meshCache.computeIfAbsent(shapesFacing) {
      buildPrintMesh(shapes, be.facing)
    }
    ctx.meshConsumer().accept(mesh)
  }

  override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<Random>, ctx: RenderContext) {
    // Quick-fail for completely empty item stacks (REI, JEI, etc)
    if (!stack.hasNbt()) {
      ctx.fallbackConsumer().accept(missingModel)
      return
    }

    try {
      // Allow this to throw when there are no shapes for the item stack
      val printData = itemCache.computeIfAbsent(stack) {
        PrintItem.printData(stack)
      }
      val shapes = printData.shapesOff

      val mesh = itemMeshCache.computeIfAbsent(shapes) {
        buildPrintMesh(shapes)
      }
      ctx.meshConsumer().accept(mesh)
    } catch (e: Exception) {
      when(e) {
        is NoPrintDataException -> { /* no-op */ }
        else -> throw e // Let this bubble up to the consumer
      }

      // Show the invalid model
      ctx.bakedModelConsumer().accept(missingModel)
      return
    }
  }

  private fun buildPrintMesh(shapes: Shapes, facing: Direction = Direction.SOUTH): Mesh? {
    val renderer = RendererAccess.INSTANCE.renderer ?: return null
    val builder = renderer.meshBuilder()
    val emitter = builder.emitter

    shapes.filter { it.texture != null }.forEach {
      buildShape(emitter, it, facing)
    }

    return builder.build()
  }

  private fun buildShape(emitter: QuadEmitter, shape: Shape, facing: Direction) {
    val bounds = shape.bounds.rotateTowards(facing)
    val texture = shape.texture

    // Discard original alpha component, then set it back to full alpha
    val tint = ((shape.tint ?: 0xFFFFFF) and 0xFFFFFF) or 0xFF000000.toInt()

    val spriteIdentifier = SpriteIdentifier(BLOCK_ATLAS_TEXTURE, texture)
    val sprite = spriteIdentifier.sprite

    // Generate the 6 faces for this Box, as a list of four vertices
    val faces = bounds.faces

    // Render each quad of the cube
    for (dir in Direction.values()) {
      // TODO: Since we are not using QuadEmitter.square(), we need to do the cullFace check ourselves
      val face = faces[dir.ordinal]
      emitter.cullFace(null)
      emitter.nominalFace(dir)
      emitter
        .pos(0, face[0])
        .pos(1, face[1])
        .pos(2, face[2])
        .pos(3, face[3])
        .spriteBake(sprite, BAKE_LOCK_UV)
        .color(0, tint)
        .color(1, tint)
        .color(2, tint)
        .color(3, tint)
        .emit()
    }
  }

  override fun getQuads(state: BlockState?, face: Direction?, random: Random): MutableList<BakedQuad> =
    mutableListOf() // Leave empty, as we are using FabricBakedModel instead

  override fun useAmbientOcclusion() = true
  override fun hasDepth() = false
  override fun isSideLit() = false
  override fun isBuiltin() = false

  override fun getTransformation(): ModelTransformation = ModelHelper.MODEL_TRANSFORM_BLOCK
  override fun getOverrides(): ModelOverrideList = ModelOverrideList.EMPTY
  override fun getParticleSprite() = sprite

  class NoPrintDataException : IllegalArgumentException("No print data found")

  companion object {
    private val reloadId = ModId("print_baked_model_cache_reloader")

    // TODO: Tune these caches
    private val meshCache = object : Cache2kBuilder<ShapesFacing, Mesh>() {}
      .entryCapacity(10000)
      .build()
    private val itemMeshCache = object : Cache2kBuilder<Shapes, Mesh>() {}
      .entryCapacity(1000)
      .build()
    private val itemCache = object : Cache2kBuilder<ItemStack, PrintData>() {}
      .entryCapacity(10000)
      .build()

    private fun clearCaches() {
      val count = meshCache.keys().size + itemMeshCache.keys().size + itemCache.keys().size

      meshCache.clear()
      itemMeshCache.clear()
      itemCache.clear()

      MinecraftClient.getInstance().player?.sendMessage(literal("")
        .append(literal("[sc-peripherals] ").formatted(YELLOW, BOLD))
        .append(literal("Cleared $count cached 3d print models")), false)
    }

    fun init() {
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
        override fun reload(manager: ResourceManager) {
          clearCaches()
        }

        override fun getFabricId() = reloadId
      })
    }
  }
}
