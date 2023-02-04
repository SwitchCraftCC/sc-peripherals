package io.sc3.peripherals.mixin;

import io.sc3.peripherals.posters.PosterState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
  @Shadow
  @Final
  private Entity entity;

  @Shadow
  @Final
  private ServerWorld world;

  @Inject(
    method = "tick",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;getHeldItemStack()Lnet/minecraft/item/ItemStack;",
      ordinal = 0,
      shift = At.Shift.AFTER
    )
  )
  private void tickItemFrame(CallbackInfo ci) {
    PosterState.tickEntityTracker((ItemFrameEntity) this.entity, this.world);
  }
}
