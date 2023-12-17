package io.sc3.peripherals.util

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods.StorageWrapper
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.minecraft.inventory.Inventory
import java.util.*

// Provides functionality similar to CC's InventoryMethods, by wrapping the methods. Functionally identical to
// Plethora's InventoryMethodsWrapper, but using CC's LuaFunction annotations instead of Plethora's BasicMethod.
abstract class InventoryPeripheral(private val inv: Inventory) : IPeripheral {
  @LuaFunction(mainThread = true)
  fun size() = inv.size()

  @LuaFunction(mainThread = true)
  fun list(): MutableMap<Int, MutableMap<String, *>> =
    inventory.list(wrapped)

  @LuaFunction(mainThread = true)
  fun getItemDetail(slot: Int): MutableMap<String, *>? =
    inventory.getItemDetail(wrapped, slot)

  @LuaFunction(mainThread = true)
  fun getItemLimit(slot: Int): Long =
    inventory.getItemLimit(wrapped, slot)

  @LuaFunction(mainThread = true)
  fun pushItems(computer: IComputerAccess, toName: String, fromSlot: Int,
                limit: Optional<Int>, toSlot: Optional<Int>) =
    inventory.pushItems(wrapped, computer, toName, fromSlot, limit, toSlot)

  @LuaFunction(mainThread = true)
  fun pullItems(computer: IComputerAccess, fromName: String, fromSlot: Int,
                limit: Optional<Int>, toSlot: Optional<Int>) =
    inventory.pullItems(wrapped, computer, fromName, fromSlot, limit, toSlot)

  @Suppress("UnstableApiUsage")
  private val wrapped: StorageWrapper
    // TODO: Can we just create a wrapper once instead of recreating it each time it's needed?
    get() = StorageWrapper(InventoryStorage.of(inv, null))

  companion object {
    private val inventory = InventoryMethods()
  }
}
