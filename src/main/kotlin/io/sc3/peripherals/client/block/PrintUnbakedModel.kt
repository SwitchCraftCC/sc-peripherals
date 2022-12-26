package io.sc3.peripherals.client.block

import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import java.util.function.Function

class PrintUnbakedModel : UnbakedModel {
  private val particleSprite = SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier("block/stone"))

  override fun getModelDependencies(): MutableCollection<Identifier> = mutableSetOf()

  override fun setParents(modelLoader: Function<Identifier, UnbakedModel>) {
    // TODO
  }

  override fun bake(baker: Baker, textureGetter: Function<SpriteIdentifier, Sprite>,
                    rotationContainer: ModelBakeSettings, modelId: Identifier): BakedModel {
    // The actual quads are generated in PrintBakedModel as this needs to be done dynamically for each block entity
    // or item stack. Just load the particle sprite here.
    val sprite = textureGetter.apply(particleSprite)
    return PrintBakedModel(sprite)
  }
}
