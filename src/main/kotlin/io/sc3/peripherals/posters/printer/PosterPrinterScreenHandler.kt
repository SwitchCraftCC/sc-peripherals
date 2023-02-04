package io.sc3.peripherals.posters.printer

import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.Registration.ModScreens.posterPrinter
import io.sc3.peripherals.util.PropertyDelegateGetter
import io.sc3.peripherals.util.ValidatingSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

const val PAPER_SLOT = 0
const val INK_SLOT = 1
const val OUTPUT_SLOT = 2

const val INV_SIZE = 3

class PosterPrinterScreenHandler(
  syncId: Int,
  playerInv: PlayerInventory,
  private val inv: Inventory,
  val pos: BlockPos,
  propertyDelegate: PropertyDelegate
) : ScreenHandler(posterPrinter, syncId) {
  private val paperSlot: Slot
  private val inkSlot: Slot
  private val outputSlot: Slot

  val ink by PropertyDelegateGetter(propertyDelegate, 1)
  val printProgress by PropertyDelegateGetter(propertyDelegate, 2)
  val maxPrintProgress by PropertyDelegateGetter(propertyDelegate, 3)

  val be by lazy { playerInv.player.world.getBlockEntity(pos) as? PosterPrinterBlockEntity }

  constructor(syncId: Int, playerInv: PlayerInventory, buf: PacketByteBuf) :
    this(syncId, playerInv, SimpleInventory(3), buf.readBlockPos(), ArrayPropertyDelegate(4))

  init {
    checkSize(inv, 3)

    paperSlot = addSlot(ValidatingSlot(inv, PAPER_SLOT, 62, 35) { s -> s.isOf(Items.PAPER) })
    inkSlot = addSlot(ValidatingSlot(inv, INK_SLOT, 17, 53) { s -> s.isOf(ModItems.inkCartridge) })
    outputSlot = addSlot(ValidatingSlot(inv, OUTPUT_SLOT, 116, 35) { false })

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

  override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
    val slot = slots[slotIndex]
    if (!slot.hasStack()) return ItemStack.EMPTY

    val existing = slot.stack
    val result = existing.copy()
    if (slotIndex < INV_SIZE) {
      // One of our own slots, insert into the player's inventory
      if (!insertItem(existing, INV_SIZE, INV_SIZE + 36, true)) return ItemStack.EMPTY
      slot.onQuickTransfer(existing, result)
    } else {
      // One of the player's inventory slots, insert into the printer inventory
      if (existing.isOf(Items.PAPER)) {
        if (!insertItem(existing, PAPER_SLOT, PAPER_SLOT + 1, false)) return ItemStack.EMPTY
      } else if (existing.isOf(ModItems.inkCartridge)) {
        if (!insertItem(existing, INK_SLOT, INK_SLOT + 1, false)) return ItemStack.EMPTY
      } else {
        // Don't allow shift-clicking into the output slot.
        return ItemStack.EMPTY
      }
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
