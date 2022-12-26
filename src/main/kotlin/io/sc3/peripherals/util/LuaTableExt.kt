package io.sc3.peripherals.util

import dan200.computercraft.api.lua.LuaValues.*

// Functions similar to LuaTable, but operating directly on Map<*, *> so it can be used on the main thread
fun Map<*, *>.getTableLong(index: Int): Long {
  val value = get(index.toDouble())
  if (value !is Number) throw badTableItem(index, "number", getType(value))

  val asDouble = value.toDouble()
  if (!asDouble.isFinite()) throw badTableItem(index, "number", getNumericType(asDouble))
  return value.toLong()
}

fun Map<*, *>.getTableLong(key: String): Long {
  val value = get(key)
  if (value !is Number) throw badField(key, "number", getType(value))

  val asDouble = value.toDouble()
  if (!asDouble.isFinite()) throw badField(key, "number", getNumericType(asDouble))
  return value.toLong()
}

fun Map<*, *>.getTableInt(index: Int) = getTableLong(index).toInt()
fun Map<*, *>.getTableInt(key: String) = getTableLong(key).toInt()
