package io.sc3.peripherals.util

import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList

fun NbtList.toFloatList(): List<Float> {
    return List(size) { i -> (get(i) as NbtFloat).floatValue() }
}
