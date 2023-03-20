package io.sc3.peripherals.client.item

import io.sc3.peripherals.posters.PosterItem
import io.sc3.peripherals.util.toFloatList
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtElement
import net.minecraft.util.ActionResult
import net.minecraft.util.math.RotationAxis

object HeadFeatureRenderer {
  fun render(
    matrices: MatrixStack,
    vertexConsumers: VertexConsumerProvider,
    entity: LivingEntity,
    itemStack: ItemStack,
    light: Int,
  ): ActionResult {
    if (itemStack.item is PosterItem) {
      val posterId = PosterItem.getPosterId(itemStack) ?: return ActionResult.PASS
      val posterState = PosterItem.getPosterState(posterId, entity.world) ?: return ActionResult.PASS

      matrices.push()

      // mc head is 8x8x8, translate to corner (4 units in each direction), and scale
      matrices.translate(-4f/16f, -8f/16f, -4f/16f)
      matrices.translate(0f, 0f, -1f/64f) // move in front of head
      matrices.scale(0.5f, 0.5f, 0.5f)

      // apply user-defined matrix
      itemStack.nbt?.let {
        if (it.contains("translate")) {
          val list = it.getList("translate", NbtElement.FLOAT_TYPE.toInt()).toFloatList()
          if (list.size == 3) matrices.translate(list[0], list[1], list[2])
        }

        if (it.contains("scale")) {
          val list = it.getList("scale", NbtElement.FLOAT_TYPE.toInt()).toFloatList()
          if (list.size == 3) matrices.scale(list[0], list[1], list[2])
        }

        if (it.contains("rotate")) {
          val list = it.getList("rotate", NbtElement.FLOAT_TYPE.toInt()).toFloatList()
          list.getOrNull(0)?.let { x -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x)) }
          list.getOrNull(1)?.let { y -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(y)) }
          list.getOrNull(2)?.let { z -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(z)) }
        }
      }

      // poster scale
      matrices.scale(1f/128f, 1f/128f, 1f/128f)

      PosterRenderer.draw(matrices, vertexConsumers, posterId, posterState, light, doubleSided = true)
      matrices.pop()

      return ActionResult.SUCCESS
    }

    return ActionResult.PASS
  }
}
