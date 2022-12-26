package io.sc3.peripherals.util

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos

abstract class BaseBlockEntity(
  type: BlockEntityType<*>,
  pos: BlockPos,
  state: BlockState
) : BlockEntity(type, pos, state) {
  open fun onBroken() {}
}
