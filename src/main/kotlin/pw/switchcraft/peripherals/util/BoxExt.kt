package pw.switchcraft.peripherals.util

import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val unitBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

val Box.volume: Int
  get() {
    val sx = xLength.toInt()
    val sy = yLength.toInt()
    val sz = zLength.toInt()
    return sx * sy * sz
  }

val Box.surfaceArea: Int
  get() {
    val sx = xLength.toInt()
    val sy = yLength.toInt()
    val sz = zLength.toInt()
    return sx * sy * 2 + sx * sz * 2 + sy * sz * 2
  }

fun Box.rotateTowards(facing: Direction): Box = rotateY(when(facing) {
  Direction.EAST -> 3
  Direction.NORTH -> 2
  Direction.WEST -> 1
  else -> 0
})

fun Box.rotateY(count: Int): Box {
  val min = Vec3d(minX - 8, minY - 8, minZ - 8)
    .rotateY(count * Math.PI.toFloat() / 2)
  val max = Vec3d(maxX - 8, maxY - 8, maxZ - 8)
    .rotateY(count * Math.PI.toFloat() / 2)

  return Box(
    (min(min.x + 8, max.x + 8) * 32).roundToInt() / 32.0,
    (min(min.y + 8, max.y + 8) * 32).roundToInt() / 32.0,
    (min(min.z + 8, max.z + 8) * 32).roundToInt() / 32.0,
    (max(min.x + 8, max.x + 8) * 32).roundToInt() / 32.0,
    (max(min.y + 8, max.y + 8) * 32).roundToInt() / 32.0,
    (max(min.z + 8, max.z + 8) * 32).roundToInt() / 32.0
  )
}

fun Box.toDiv16VoxelShape(): VoxelShape =
  VoxelShapes.cuboid(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0)

val Box.faces: List<List<Vec3f>>
  get() = unitCube.map { face -> face.map { vertex -> Vec3f(
    max(minX.toFloat() / 16.0f, min(maxX.toFloat() / 16.0f, vertex.x)),
    max(minY.toFloat() / 16.0f, min(maxY.toFloat() / 16.0f, vertex.y)),
    max(minZ.toFloat() / 16.0f, min(maxZ.toFloat() / 16.0f, vertex.z))
  )}}

fun intBox(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int) =
  Box(minX.toDouble(), minY.toDouble(), minZ.toDouble(), maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())

fun randBox() = intBox(
  (0..7).random(), (0..7).random(), (0..7).random(),
  (8..15).random(), (8..15).random(), (8..15).random(),
)
