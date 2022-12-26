package io.sc3.peripherals.prints.printer

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.Registration.ModScreens.printer
import io.sc3.peripherals.util.PropertyDelegateGetter
import io.sc3.peripherals.util.ValidatingSlot

const val CHAMELIUM_SLOT = 0
const val INK_SLOT = 1
const val OUTPUT_SLOT = 2

const val INV_SIZE = 3

class PrinterScreenHandler(
  syncId: Int,
  playerInv: PlayerInventory,
  private val inv: Inventory,
  propertyDelegate: PropertyDelegate
) : ScreenHandler(printer, syncId) {
  private val chameliumSlot: Slot
  private val inkSlot: Slot
  private val outputSlot: Slot

  val chamelium by PropertyDelegateGetter(propertyDelegate, 0)
  val ink by PropertyDelegateGetter(propertyDelegate, 1)
  val printProgress by PropertyDelegateGetter(propertyDelegate, 2)
  val maxPrintProgress by PropertyDelegateGetter(propertyDelegate, 3)

  constructor(syncId: Int, playerInv: PlayerInventory) :
    this(syncId, playerInv, SimpleInventory(3), ArrayPropertyDelegate(4))

  init {
    checkSize(inv, 3)

    // TODO: Validating slots
    chameliumSlot = addSlot(ValidatingSlot(inv, CHAMELIUM_SLOT, 17, 17) {
      s -> s.isOf(ModItems.chamelium) || s.isOf(ModItems.print)
    })
    inkSlot = addSlot(ValidatingSlot(inv, INK_SLOT, 17, 53) { s -> s.isOf(ModItems.inkCartridge) })
    outputSlot = addSlot(ValidatingSlot(inv, OUTPUT_SLOT, 139, 35) { false })

    // Player inventory slots
    for (y in 0 until 3) {
      for (x in 0 until 9) {
        addSlot(Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18))
      }
    }

    // Player hotbar
    for (i in 0 until 9) {
      addSlot(Slot(playerInv, i, 8 + i * 18, 142))
    }

    // Property delegate to synchronise chamelium, ink levels
    addProperties(propertyDelegate)
  }

  override fun canUse(player: PlayerEntity) = inv.canPlayerUse(player)

  override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
    val slot = slots[index]
    if (!slot.hasStack()) return ItemStack.EMPTY

    val existing = slot.stack.copy()
    val result = existing.copy()
    if (index < INV_SIZE) {
      // One of our own slots, insert into the player's inventory
      if (!insertItem(existing, INV_SIZE, INV_SIZE + 36, true)) return ItemStack.EMPTY
    } else {
      // One of the player's inventory slots, insert into the printer inventory
      if (!insertItem(existing, 0, INV_SIZE, false)) return ItemStack.EMPTY
    }

    if (existing.isEmpty) {
      slot.stack = ItemStack.EMPTY
    } else {
      slot.markDirty()
    }

    if (existing.count == result.count) return ItemStack.EMPTY

    slot.onTakeItem(player, existing)
    return result
  }
}
