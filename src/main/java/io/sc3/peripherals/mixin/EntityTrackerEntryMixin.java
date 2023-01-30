package io.sc3.peripherals.mixin;

import io.sc3.library.networking.ScLibraryPacket;
import io.sc3.peripherals.Registration;
import io.sc3.peripherals.posters.PosterItem;
import io.sc3.peripherals.posters.PosterState;
import io.sc3.peripherals.posters.printer.PosterPrinterBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

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
    ItemFrameEntity entity = (ItemFrameEntity) this.entity;
    ItemStack itemStack = entity.getHeldItemStack();

    if (itemStack.getItem() == Registration.ModItems.INSTANCE.getPoster()) {
      String id = PosterItem.Companion.getPosterId(itemStack);
      if (id == null) return;

      PosterState posterState = PosterItem.Companion.getPosterState(id, this.world);
      if (posterState != null) {

        // TODO: Don't send to players who can't see the poster

        for (ServerPlayerEntity serverPlayerEntity : this.world.getPlayers()) {
          posterState.update(serverPlayerEntity);
          ScLibraryPacket packet = posterState.getPlayerUpdatePacket(id, serverPlayerEntity);
          if (packet != null) {
            serverPlayerEntity.networkHandler.sendPacket(packet.toS2CPacket());
          }
        }

        posterState.pruneTrackers(itemStack);
      }
    }
  }
}
