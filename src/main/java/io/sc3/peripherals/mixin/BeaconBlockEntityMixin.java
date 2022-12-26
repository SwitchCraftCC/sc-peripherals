package io.sc3.peripherals.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.sc3.peripherals.prints.PrintBlock;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {
  @Redirect(
    method="updateLevel",
    at=@At(
      value="INVOKE",
      target="Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
    )
  )
  private static BlockState getBeaconBaseBlockState(World world, BlockPos pos) {
    // If this is a beacon base print block, pretend to be an iron block
    return PrintBlock.beaconBlockState(world, pos);
  }
}
