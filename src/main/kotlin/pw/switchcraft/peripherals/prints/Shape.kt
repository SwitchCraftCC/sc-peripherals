package pw.switchcraft.peripherals.prints

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import pw.switchcraft.library.ext.byteToDouble
import pw.switchcraft.library.ext.optInt
import pw.switchcraft.library.ext.optString
import pw.switchcraft.library.ext.putOptInt

data class Shape(
  val bounds: Box,
  val texture: Identifier? = null,
  val tint: Int? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Shape

    if (bounds != other.bounds) return false
    if (texture != other.texture) return false
    if (tint != other.tint) return false

    return true
  }

  override fun hashCode(): Int {
    var result = bounds.hashCode()
    result = 31 * result + (texture?.hashCode() ?: 0)
    result = 31 * result + (tint ?: 0)
    return result
  }

  fun toNbt(): NbtCompound {
    val nbt = NbtCompound()
    nbt.putByte("minX", bounds.minX.toInt().coerceIn(0, 16).toByte())
    nbt.putByte("minY", bounds.minY.toInt().coerceIn(0, 16).toByte())
    nbt.putByte("minZ", bounds.minZ.toInt().coerceIn(0, 16).toByte())
    nbt.putByte("maxX", bounds.maxX.toInt().coerceIn(0, 16).toByte())
    nbt.putByte("maxY", bounds.maxY.toInt().coerceIn(0, 16).toByte())
    nbt.putByte("maxZ", bounds.maxZ.toInt().coerceIn(0, 16).toByte())
    nbt.putString("tex", texture?.toString() ?: "")
    nbt.putOptInt("tint", tint)
    return nbt
  }

  companion object {
    fun fromNbt(nbt: NbtCompound) = Shape(
      Box(
        nbt.byteToDouble("minX"), nbt.byteToDouble("minY"), nbt.byteToDouble("minZ"),
        nbt.byteToDouble("maxX"), nbt.byteToDouble("maxY"), nbt.byteToDouble("maxZ")
      ),
      nbt.optString("tex")?.let { Identifier(it) },
      nbt.optInt("tint")
    )
  }
}
