package io.sc3.peripherals.prints

import io.sc3.library.ext.optString
import io.sc3.library.ext.putOptString
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import net.fabricmc.fabric.api.util.NbtType.COMPOUND
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape

const val MAX_LABEL_LENGTH = 48
const val MAX_TOOLTIP_LENGTH = 256

data class PrintData(
  private val initialLabel: String? = null,
  var tooltip: String? = null,

  var isButton: Boolean = false,
  var collideWhenOn: Boolean = true,
  var collideWhenOff: Boolean = true,

  var lightLevel: Int = 0,
  var redstoneLevel: Int = 0,
  var isBeaconBlock: Boolean = false,

  val shapesOff: Shapes = Shapes(),
  val shapesOn: Shapes = Shapes(),
) {
  var label: String? = initialLabel
    set(value) {
      field = value?.takeIf { it.isValidLabel() }
      labelText = field?.let { Text.of(it) }
    }

  var labelText: Text? = initialLabel?.let { Text.of(it) }
    private set

  private val voxelShapesOff = mutableMapOf<Direction, VoxelShape>()
  private val voxelShapesOn = mutableMapOf<Direction, VoxelShape>()

  fun voxelShape(direction: Direction, on: Boolean): VoxelShape {
    val voxelShapes = if (on) voxelShapesOn else voxelShapesOff
    return voxelShapes.getOrPut(direction) {
      val shapes = if (on) shapesOn else shapesOff
      shapes.toVoxelShape(direction)
    }
  }

  fun computeCosts(): Pair<Int, Int>? {
    val totalVolume = shapesOff.totalVolume + shapesOn.totalVolume
    val totalSurface = shapesOff.totalSurfaceArea + shapesOn.totalSurfaceArea

    // Invalid print data
    if (totalVolume <= 0) return null

    val redstoneCost = if (redstoneLevel in 1..14) customRedstoneCost else 0
    val noclipCost = if (!collideWhenOff || !collideWhenOn) noclipCostMultiplier else 1

    val chamelium = ((totalVolume / 2.0).coerceAtLeast(1.0) + redstoneCost) * noclipCost
    val ink = (totalSurface / 6.0).coerceAtLeast(1.0)

    return chamelium.toInt() to ink.toInt()
  }

  fun toNbt(): NbtCompound {
    val nbt = NbtCompound()
    nbt.putOptString("label", label?.takeIf { it.isValidLabel() })
    nbt.putOptString("tooltip", tooltip?.takeIf { it.isValidTooltip() })
    nbt.putBoolean("isButton", isButton)
    nbt.putBoolean("collideWhenOn", collideWhenOn)
    nbt.putBoolean("collideWhenOff", collideWhenOff)
    nbt.putInt("lightLevel", lightLevel)
    nbt.putInt("redstoneLevel", redstoneLevel)
    nbt.putBoolean("isBeaconBlock", isBeaconBlock)
    nbt.put("shapesOff", shapesOff.toNbt())
    nbt.put("shapesOn", shapesOn.toNbt())
    return nbt
  }

  companion object {
    val customRedstoneCost: Int = config.get("printer.custom_redstone_cost")
    val noclipCostMultiplier: Int = config.get("printer.noclip_cost_multiplier")

    fun fromNbt(nbt: NbtCompound) = PrintData(
      initialLabel = nbt.optString("label")?.takeIf { it.isValidLabel() },
      tooltip = nbt.optString("tooltip"),
      isButton = nbt.getBoolean("isButton"),
      collideWhenOn = nbt.getBoolean("collideWhenOn"),
      collideWhenOff = nbt.getBoolean("collideWhenOff"),
      lightLevel = nbt.getInt("lightLevel"),
      redstoneLevel = nbt.getInt("redstoneLevel"),
      isBeaconBlock = nbt.getBoolean("isBeaconBlock"),
      shapesOff = nbt.getShapeSet("shapesOff"),
      shapesOn = nbt.getShapeSet("shapesOn"),
    )

    private fun NbtCompound.getShapeSet(key: String): Shapes =
      getList(key, COMPOUND)
        .map { Shape.fromNbt(it as NbtCompound) }
        .toCollection(Shapes())

    private fun String?.isValidLabel() = this?.length in 1..MAX_LABEL_LENGTH
    private fun String?.isValidTooltip() = this?.length in 1..MAX_TOOLTIP_LENGTH
  }
}
