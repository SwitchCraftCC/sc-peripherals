package io.sc3.peripherals.prints

import net.minecraft.nbt.NbtList
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import io.sc3.library.ext.rotateTowards
import io.sc3.library.ext.surfaceArea
import io.sc3.library.ext.toDiv16VoxelShape
import io.sc3.library.ext.volume

class Shapes : HashSet<Shape>() {
  private val hashCode by lazy { super.hashCode() }
  override fun hashCode() = hashCode
  override fun equals(other: Any?) = other is Shapes && other.hashCode == hashCode && super.equals(other)

  val totalVolume
    get() = sumOf { it.bounds.volume }
  val totalSurfaceArea
    get() = sumOf { it.bounds.surfaceArea }

  fun toNbt(): NbtList {
    // Sort the shapes by hashcode to make sure they are always in the same order - NBT comparison is order-sensitive
    val sortedShapes = sortedBy { it.hashCode() }
    val list = NbtList()
    list.addAll(sortedShapes.map(Shape::toNbt))
    return list
  }

  fun toVoxelShape(direction: Direction): VoxelShape = if (isEmpty()) {
    // TODO: empty() makes more sense, but getting a print into this invalid state results in ghost blocks, which are
    //   never cool. Is this okay?
    VoxelShapes.fullCube()
  } else {
    map { it.bounds.rotateTowards(direction).toDiv16VoxelShape() }
      .reduce { acc, shape -> VoxelShapes.union(acc, shape) }
  }
}

data class ShapesFacing(val shapes: Shapes, val facing: Direction) {
  private val hashCode by lazy {
    31 * shapes.hashCode() + facing.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ShapesFacing

    if (shapes != other.shapes) return false
    if (facing != other.facing) return false

    return true
  }

  override fun hashCode() = hashCode
}
