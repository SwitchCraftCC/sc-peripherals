package io.sc3.peripherals.prints.printer

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.LuaValues.*
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.lua.MethodResult.of
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import io.sc3.library.ext.intBox
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import io.sc3.peripherals.prints.PrintData
import io.sc3.peripherals.prints.Shape
import io.sc3.peripherals.util.getTableInt
import net.minecraft.util.Identifier
import org.squiddev.cobalt.LuaString
import java.util.*

class PrinterPeripheral(val be: PrinterBlockEntity) : IPeripheral {
  override fun getType() = "3d_printer"

  @LuaFunction(mainThread = true)
  fun reset() {
    be.data = PrintData()
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getLabel(): String? = be.data.label

  @LuaFunction(mainThread = true)
  fun setLabel(args: IArguments) {
    be.data.label = args.optUtf8String(0)?.let { PrintData.sanitiseLabel(it) }
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getTooltip(): String? = be.data.tooltip

  @LuaFunction(mainThread = true)
  fun setTooltip(args: IArguments) {
    be.data.tooltip = args.optUtf8String(0)?.let { PrintData.sanitiseTooltip(it) }
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getLightLevel(): Int = be.data.lightLevel

  @LuaFunction(mainThread = true)
  fun setLightLevel(lightLevel: Int) {
    be.data.lightLevel = lightLevel.coerceIn(0, maxBaseLightLevel)
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getRedstoneLevel(): Int = be.data.redstoneLevel

  @LuaFunction(mainThread = true)
  fun setRedstoneLevel(redstoneLevel: Int) {
    be.data.redstoneLevel = redstoneLevel.coerceIn(0, 15)
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getButtonMode(): Boolean = be.data.isButton

  @LuaFunction(mainThread = true)
  fun setButtonMode(isButton: Boolean) {
    be.data.isButton = isButton
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun isCollidable(): MethodResult = of(be.data.collideWhenOff, be.data.collideWhenOn)

  @LuaFunction(mainThread = true)
  fun setCollidable(collideWhenOff: Boolean, collideWhenOn: Boolean) {
    be.data.collideWhenOff = collideWhenOff
    be.data.collideWhenOn = collideWhenOn
    be.dataUpdated()
  }
  
  @LuaFunction(mainThread = true)
  fun getShapeCount(): MethodResult = of(be.data.shapesOff.size, be.data.shapesOn.size)

  @LuaFunction
  fun getMaxShapeCount(): Int = maxShapes

  @LuaFunction(mainThread = true)
  fun addShape(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, texture: String,
               state: Optional<Boolean>, tint: Optional<Int>) {
    if (be.data.shapesOff.size >= maxShapes || be.data.shapesOn.size >= maxShapes) {
      throw LuaException("Too many shapes")
    }

    val box = intBox(
      minX.coerceIn(0, 16), minY.coerceIn(0, 16), minZ.coerceIn(0, 16),
      maxX.coerceIn(0, 16), maxY.coerceIn(0, 16), maxZ.coerceIn(0, 16)
    )
    val textureId = Identifier(texture.take(64))
    val fixedTint = tint.orElse(0xFFFFFF) and 0xFFFFFF // Discard alpha component

    if (box.xLength <= 0 || box.yLength <= 0 || box.zLength <= 0) {
      throw LuaException("Empty block")
    }

    val shapes = if (state.orElse(false)) be.data.shapesOn else be.data.shapesOff
    shapes.add(Shape(box, textureId, fixedTint))

    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun addShapes(shapes: Map<*, *>) {
    shapes.forEach { (i, shape) ->
      if (i !is Double || !i.isFinite()) throw LuaException("Invalid shape table")
      if (shape !is Map<*, *>) throw badTableItem(i.toInt(), "table", getType(shape))

      val texVal = shape["texture"]
      val texture = texVal as? String ?: throw badField("texture", "string", getType(texVal))

      addShape(
        shape.getTableInt(1), shape.getTableInt(2), shape.getTableInt(3),
        shape.getTableInt(4), shape.getTableInt(5), shape.getTableInt(6),
        texture,
        Optional.of(shape["state"] as? Boolean ?: false),
        if (shape.containsKey("tint")) Optional.of(shape.getTableInt("tint")) else Optional.empty()
      )
    }
  }

  @LuaFunction(mainThread = true)
  fun commit(count: Int): Boolean {
    if (!be.canPrint) throw LuaException("Model is invalid")
    be.printCount = count.coerceIn(0, Int.MAX_VALUE)
    return (be.printCount > 0).also { be.printing = it }
  }

  @LuaFunction(mainThread = true)
  fun stop() {
    be.printCount = 0
    be.printing = false
  }

  @LuaFunction(mainThread = true)
  fun status(): MethodResult {
    val (status, progress) = printerStatus(be)
    return of(status, progress)
  }

  @LuaFunction(mainThread = true)
  fun getChameliumLevel() = of(be.chamelium, PrinterBlockEntity.maxChamelium)
  
  @LuaFunction(mainThread = true)
  fun getInkLevel() = of(be.ink, PrinterBlockEntity.maxInk)

  override fun attach(computer: IComputerAccess) {
    be.computers.add(computer)
  }

  override fun detach(computer: IComputerAccess) {
    be.computers.remove(computer)
  }

  @Suppress("CovariantEquals")
  override fun equals(other: IPeripheral?): Boolean = this == other

  companion object {
    private const val printStatusEvent = "3d_printer_state" // args: status:string
    private const val printCompleteEvent = "3d_printer_complete" // args: remaining:number

    val maxBaseLightLevel: Int = config.get("printer.max_base_light_level")
    val maxShapes: Int = config.get("printer.max_shapes")

    fun printerStatus(be: PrinterBlockEntity): Pair<String, Any> {
      return if (be.printing || be.printProgress > 0) Pair("busy", be.printProgress)
      else if (be.canPrint) Pair("idle", true)
      else Pair("idle", false)
    }

    fun sendPrintStatusEvent(be: PrinterBlockEntity) {
      val (status) = printerStatus(be)
      be.computers.forEach { it.queueEvent(printStatusEvent, status) }
    }

    fun sendPrintCompleteEvent(be: PrinterBlockEntity) {
      val remaining = be.printCount
      be.computers.forEach { it.queueEvent(printCompleteEvent, remaining) }
    }

    private fun IArguments.optUtf8String(index: Int): String? {
      val buf = optBytes(index).orElse(null) ?: return null
      val bytes = ByteArray(buf.capacity().coerceAtMost(1024))
      buf.get(bytes)
      return LuaString.decodeAsUtf8(bytes, 0, bytes.size)
    }
  }
}
