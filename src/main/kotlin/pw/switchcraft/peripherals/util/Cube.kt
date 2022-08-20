package pw.switchcraft.peripherals.util

import net.minecraft.util.math.Vec3f

val unitCube: List<List<Vec3f>> = listOf(
  listOf(Vec3f(0.0f, 0.0f, 1.0f), Vec3f(0.0f, 0.0f, 0.0f), Vec3f(1.0f, 0.0f, 0.0f), Vec3f(1.0f, 0.0f, 1.0f)),
  listOf(Vec3f(0.0f, 1.0f, 0.0f), Vec3f(0.0f, 1.0f, 1.0f), Vec3f(1.0f, 1.0f, 1.0f), Vec3f(1.0f, 1.0f, 0.0f)),
  listOf(Vec3f(1.0f, 1.0f, 0.0f), Vec3f(1.0f, 0.0f, 0.0f), Vec3f(0.0f, 0.0f, 0.0f), Vec3f(0.0f, 1.0f, 0.0f)),
  listOf(Vec3f(0.0f, 1.0f, 1.0f), Vec3f(0.0f, 0.0f, 1.0f), Vec3f(1.0f, 0.0f, 1.0f), Vec3f(1.0f, 1.0f, 1.0f)),
  listOf(Vec3f(0.0f, 1.0f, 0.0f), Vec3f(0.0f, 0.0f, 0.0f), Vec3f(0.0f, 0.0f, 1.0f), Vec3f(0.0f, 1.0f, 1.0f)),
  listOf(Vec3f(1.0f, 1.0f, 1.0f), Vec3f(1.0f, 0.0f, 1.0f), Vec3f(1.0f, 0.0f, 0.0f), Vec3f(1.0f, 1.0f, 0.0f))
)
