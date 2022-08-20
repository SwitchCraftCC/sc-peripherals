package pw.switchcraft.peripherals.item

import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text.literal
import net.minecraft.text.Text.translatable
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting.*
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import pw.switchcraft.peripherals.util.BaseItem

class TextureAnalyzerItem(settings: Settings) : BaseItem("texture_analyzer", settings) {
  private val copyText = translatable("$translationKey.copy")

  override fun useOnBlock(ctx: ItemUsageContext): ActionResult {
    // Only run on the client.
    if (!ctx.world.isClient) return ActionResult.PASS

    val blockState = ctx.world.getBlockState(ctx.blockPos)
    val brm = MinecraftClient.getInstance().blockRenderManager

    val model = brm.getModel(blockState)
    val rand = Random.create()

    // Iterate all the directions, and null, because BasicBlockModels only return sprites if they are given a valid
    // face, and other custom models may return different values if they are passed `null`. We want to get as many
    // textures as possible.
    val sides = listOf(*Direction.values(), null)
    val sprites = sides.flatMap {
      model.getQuads(blockState, it, rand)
        .mapNotNull { quad -> quad.sprite?.id?.toString() }
    }.toSet()

    val name = blockState.toString()

    if (sprites.isEmpty()) {
      ctx.player?.sendMessage(translatable("$translationKey.no_textures", name).formatted(RED))
      return ActionResult.SUCCESS
    }

    val text = translatable("$translationKey.title", name).formatted(YELLOW)
    sprites.forEach {
      text.append(literal("\n- ").formatted(GRAY))
      text.append(literal(it).formatted(WHITE).styled { s ->
        s.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, copyText))
          .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, it))
      })
    }

    ctx.player?.sendMessage(text, false)

    return ActionResult.SUCCESS
  }
}
