package io.sc3.peripherals.posters

import com.mojang.blaze3d.systems.RenderSystem
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.client.item.PosterRenderer
import io.sc3.peripherals.client.item.PosterRenderer.POSTER_BACKGROUND_RES
import io.sc3.peripherals.util.BaseItem
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.item.TooltipData
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.*

class PosterItem(settings: Settings) : BaseItem("poster", settings) {
  data class PosterTooltipData(val stack: ItemStack): TooltipData

  override fun getTooltipData(stack: ItemStack): Optional<TooltipData> = Optional.of(PosterTooltipData(stack))

  override fun getName(stack: ItemStack): Text = printData(stack)?.labelText ?: super.getName(stack)

  override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
    val id = getPosterId(stack)

    val data = printData(stack) ?: return
    data.tooltip?.let { tooltip.add(Text.literal(it)) }

    if (context.isAdvanced) {
      if (id != null) {
        tooltip.add(Text.translatable("${translationKey}.id", id.take(8)).formatted(Formatting.GRAY))
      } else {
        tooltip.add(Text.translatable("${translationKey}.unknown").formatted(Formatting.GRAY))
      }
    }
  }

  data class PosterTooltipComponent(val stack: ItemStack, val world: World?) : TooltipComponent {
    override fun drawItems(
      textRenderer: TextRenderer,
      tooltipX: Int,
      tooltipY: Int,
      ctx: DrawContext
    ) {
      val posterId = getPosterId(stack) ?: return
      val posterState = getPosterState(posterId, world) ?: return
      val matrices = ctx.matrices
      val pad = 7
      val size = 135 + pad
      val scale = 0.5f
      matrices.push()
      matrices.translate(tooltipX + 3.0, tooltipY + 3.0, 500.0)
      matrices.scale(scale, scale, 1f)
      RenderSystem.enableBlend()
      ctx.drawTexture(POSTER_BACKGROUND_RES, -pad, -pad, -1, 0f, 0f, size, size, size, size)
      val buffer: BufferBuilder = Tessellator.getInstance().buffer
      val immediateBuffer = VertexConsumerProvider.immediate(buffer)
      PosterRenderer.draw(matrices, immediateBuffer, posterId, posterState, 240)
      immediateBuffer.draw()
      matrices.pop()
    }

    override fun getHeight(): Int {
      val posterId = getPosterId(stack)
      val posterState = getPosterState(posterId, world)
      return if (posterState != null) 75 else 0
    }

    override fun getWidth(textRenderer: TextRenderer) = 72
  }

  companion object {
    const val POSTER_KEY = "poster"

    fun getPosterName(posterId: String) = "posters/${posterId.take(2)}/$posterId"

    fun getIdFromName(posterName: String) = posterName.substringAfterLast('/')

    internal fun clientInit() {
      TooltipComponentCallback.EVENT.register {
        if (it is PosterTooltipData) {
          PosterTooltipComponent(it.stack, MinecraftClient.getInstance().world)
        } else {
          null
        }
      }
    }

    fun create(world: World, data: PosterPrintData): ItemStack {
      val itemStack = ItemStack(ModItems.poster)
      data.posterId.let { id ->
        if (id != null) {
          setPosterId(itemStack, id, data)
        } else {
          createPosterState(itemStack, world, data)
        }
      }

      return itemStack
    }

    private fun setPosterId(stack: ItemStack, id: String, data: PosterPrintData) {
      with (stack.orCreateNbt) {
        putString(POSTER_KEY, id)
        copyFrom(data.toItemNbt())
      }
    }

    private fun createPosterState(
      stack: ItemStack,
      world: World,
      data: PosterPrintData
    ): PosterState {
      val (i, state) = allocatePosterId(world, data)
      setPosterId(stack, i, data)
      return state
    }

    private fun allocatePosterId(
      world: World, data: PosterPrintData
    ): Pair<String, PosterState> {
      val posterState = PosterState().also {
        for (x in 0 until 128) {
          for (y in 0 until 128) {
            it.setColor(x, y, data.colors[x + y * 128])
          }
        }

        for (i in 0 until data.palette.size) {
          it.palette[i] = data.palette[i]
        }
      }

      val i = allocateHash(world, posterState)

      world.putPosterState(getPosterName(i), posterState)
      return i to posterState
    }

    private fun allocateHash(world: World, data: PosterState): String {
      val contents = ByteArrayOutputStream().use { baos ->
        DataOutputStream(baos).use { dos ->
          NbtIo.write(data.writeNbt(NbtCompound()), dos)
          baos.toByteArray()
        }
      }

      var disambiguation = 0
      while (true) {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(contents + disambiguation.toString().toByteArray())
        val hex = hash.joinToString("") { "%02x".format(it) }

        val existingData = world.getPosterState(getPosterName(hex))
        if (existingData == null || existingData == data) {
          return hex
        }

        disambiguation += 1
        if (disambiguation > 128) {
          throw IllegalStateException("Likely infinite loop allocating poster ID, this should never happen!")
        }
      }
    }

    fun getPosterId(stack: ItemStack?): String? {
      return stack?.nbt?.let {
        if (it.contains(POSTER_KEY, NbtElement.STRING_TYPE.toInt())) it.getString(POSTER_KEY) else null
      }
    }

    fun getPosterState(id: String?, world: World?): PosterState? {
      return if (id == null) null else world?.getPosterState(getPosterName(id))
    }

    fun printData(stack: ItemStack): PosterPrintData?
      = stack.nbt?.let { PosterPrintData.fromNbt(it) }
  }
}
