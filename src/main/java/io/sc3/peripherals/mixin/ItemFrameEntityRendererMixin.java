package io.sc3.peripherals.mixin;

import io.sc3.peripherals.Registration;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameEntityRenderer.class)
public class ItemFrameEntityRendererMixin {
  @Redirect(
    method="getModelId",
    at=@At(
      value="INVOKE",
      target="Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
      ordinal = 0
    )
  )
  private boolean isOf(net.minecraft.item.ItemStack stack, net.minecraft.item.Item item) {
    return stack.isOf(item) || stack.isOf(Registration.ModItems.INSTANCE.getPoster());
  }
}
