package io.sc3.peripherals.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public abstract class HeadFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T> & ModelWithHead> {
  @Inject(
    method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/render/entity/feature/HeadFeatureRenderer;translate(Lnet/minecraft/client/util/math/MatrixStack;Z)V",
      shift = At.Shift.BEFORE
    ),
    cancellable = true
  )
  public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
    ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
    if (io.sc3.peripherals.client.item.HeadFeatureRenderer.INSTANCE.render(matrixStack, vertexConsumerProvider, livingEntity, itemStack, i) != ActionResult.PASS) {
      matrixStack.pop(); // the pop gets skipped if we cancel the method
      ci.cancel();
    }
  }
}
