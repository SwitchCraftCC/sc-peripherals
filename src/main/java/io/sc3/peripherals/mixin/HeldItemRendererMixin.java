package io.sc3.peripherals.mixin;

import io.sc3.peripherals.Registration;
import io.sc3.peripherals.client.item.PosterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
  @Accessor
  abstract MinecraftClient getClient();

  @Redirect(
    method="renderFirstPersonItem",
    at=@At(
      value="INVOKE",
      target="Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
      ordinal = 0
    )
  )
  private boolean isOf(net.minecraft.item.ItemStack stack, net.minecraft.item.Item item) {
    return stack.isOf(item) || stack.isOf(Registration.ModItems.INSTANCE.getPoster());
  }

  @Inject(method = "renderFirstPersonMap", at = @At(
    value = "INVOKE",
    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
    ordinal = 1,
    shift = At.Shift.AFTER
  ), cancellable = true)
  private void renderFirstPersonMap(
    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int swingProgress, ItemStack stack, CallbackInfo ci
  ) {
    if (stack.isOf(Registration.ModItems.INSTANCE.getPoster())) {
      PosterRenderer.renderFirstPersonMap(matrices, vertexConsumers, swingProgress, stack, this.getClient().world);
      ci.cancel();
    }
  }
}
