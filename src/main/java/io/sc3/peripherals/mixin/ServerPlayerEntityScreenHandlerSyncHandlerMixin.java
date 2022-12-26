package pw.switchcraft.peripherals.mixin;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.switchcraft.peripherals.prints.printer.PrinterScreenHandler;
import pw.switchcraft.peripherals.util.ScreenHandlerPropertyUpdateIntS2CPacket;

@Mixin(targets = "net.minecraft.server.network.ServerPlayerEntity$1")
public abstract class ServerPlayerEntityScreenHandlerSyncHandlerMixin {
  @Shadow
  @Final
  private ServerPlayerEntity field_29182;

  /**
   * Sends an int for a property update value instead of a short
   */
  @Inject(method = "sendPropertyUpdate", at = @At("HEAD"), cancellable = true)
  public void sendPropertyUpdate(ScreenHandler handler, int property, int value, CallbackInfo ci) {
    if (handler instanceof PrinterScreenHandler) {
      ci.cancel();
      new ScreenHandlerPropertyUpdateIntS2CPacket(
        handler.syncId,
        property,
        value
      ).send(field_29182.networkHandler);
    }
  }
}
