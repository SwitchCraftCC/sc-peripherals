package pw.switchcraft.peripherals.client.block

import com.mojang.datafixers.util.Pair
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier
import java.util.function.Function

class PrintUnbakedModel : UnbakedModel {
  private val particleSprite = SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier("block/stone"))

  override fun getModelDependencies(): MutableCollection<Identifier> = mutableSetOf()

  override fun getTextureDependencies(
    unbakedModelGetter: Function<Identifier, UnbakedModel>,
    unresolvedTextureReferences: MutableSet<Pair<String, String>>
  ): MutableCollection<SpriteIdentifier> =
    mutableSetOf(particleSprite)

  override fun bake(loader: ModelLoader, textureGetter: Function<SpriteIdentifier, Sprite>,
                    rotationContainer: ModelBakeSettings, modelId: Identifier): BakedModel {
    // The actual quads are generated in PrintBakedModel as this needs to be done dynamically for each block entity
    // or item stack. Just load the particle sprite here.
    val sprite = textureGetter.apply(particleSprite)
    return PrintBakedModel(sprite)
  }
}
