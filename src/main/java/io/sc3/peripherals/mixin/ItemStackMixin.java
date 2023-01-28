package io.sc3.peripherals.mixin;

import io.sc3.peripherals.prints.PrintItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
  @Inject(
    method = "hasCustomName",
    at = @At("HEAD"),
    cancellable = true
  )
  private void hasCustomName(CallbackInfoReturnable<Boolean> cir) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof PrintItem) {
      cir.setReturnValue(PrintItem.hasCustomName(stack));
    }
  }

  @Inject(
    method = "getName",
    at = @At("HEAD"),
    cancellable = true
  )
  private void getName(CallbackInfoReturnable<Text> cir) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof PrintItem) {
      cir.setReturnValue(PrintItem.getCustomName(stack));
    }
  }

  @Inject(
    method = "setCustomName",
    at = @At("HEAD"),
    cancellable = true
  )
  private void setCustomName(Text name, CallbackInfoReturnable<ItemStack> cir) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof PrintItem) {
      PrintItem.setCustomName(stack, name);
      cir.setReturnValue(stack);
    }
  }

  @Inject(
    method = "removeCustomName",
    at = @At("HEAD"),
    cancellable = true
  )
  private void removeCustomName(CallbackInfo ci) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof PrintItem) {
      PrintItem.removeCustomName(stack);
      // Vanilla does an extra check here to clean empty NBT, but removing a label shouldn't clear the rest of the
      // print NBT.
      ci.cancel();
    }
  }
}
