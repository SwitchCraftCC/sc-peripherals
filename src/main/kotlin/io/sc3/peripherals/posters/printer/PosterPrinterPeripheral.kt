package io.sc3.peripherals.posters.printer

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.LuaValues.*
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.lua.MethodResult.of
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import io.sc3.peripherals.posters.PosterPrintData
import io.sc3.peripherals.prints.MAX_LABEL_LENGTH
import io.sc3.peripherals.prints.MAX_TOOLTIP_LENGTH
import io.sc3.peripherals.prints.printer.PrinterBlockEntity
import java.util.*

class PosterPrinterPeripheral(val be: PosterPrinterBlockEntity) : IPeripheral {
  override fun getType() = "poster_printer"

  @LuaFunction(mainThread = true)
  fun reset() {
    be.data = PosterPrintData()
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getLabel(): String? = be.data.label

  @LuaFunction(mainThread = true)
  fun setLabel(label: Optional<String>) {
    be.data.label = label.map { it.take(MAX_LABEL_LENGTH) }.orElse(null)
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun getTooltip(): String? = be.data.tooltip

  @LuaFunction(mainThread = true)
  fun setTooltip(tooltip: Optional<String>) {
    be.data.tooltip = tooltip.map { it.take(MAX_TOOLTIP_LENGTH) }.orElse(null)
    be.dataUpdated()
  }

  @LuaFunction
  fun setPaletteColor(index: Int, red: Int, green: Int, blue: Int) {
    if (index < 1 || index > 64) {
      throw LuaException("Invalid palette index")
    }
    be.data.posterId = null
    be.data.palette[index - 1] = (red shl 16) or (green shl 8) or blue
    be.dataUpdated()
  }

  @LuaFunction
  fun setPixel(x: Int, y: Int, color: Int) {
    if (x !in 1..128 || y !in 1..128) throw LuaException("Invalid pixel coordinates")
    be.data.posterId = null
    be.data.colors[(x-1) + (y-1) * 128] = color
    be.dataUpdated()
  }

  @LuaFunction(mainThread = true)
  fun commit(count: Int): Boolean {
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

    fun printerStatus(be: PosterPrinterBlockEntity): Pair<String, Any> {
      return if (be.printing || be.printProgress > 0) Pair("busy", be.printProgress)
      else Pair("idle", true)
    }

    fun sendPrintStatusEvent(be: PosterPrinterBlockEntity) {
      val (status) = printerStatus(be)
      be.computers.forEach { it.queueEvent(printStatusEvent, status) }
    }

    fun sendPrintCompleteEvent(be: PosterPrinterBlockEntity) {
      val remaining = be.printCount
      be.computers.forEach { it.queueEvent(printCompleteEvent, remaining) }
    }
  }
}
