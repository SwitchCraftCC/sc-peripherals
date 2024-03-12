package io.sc3.peripherals.prints.printer

import dan200.computercraft.api.peripheral.IComputerAccess
import io.sc3.library.ext.optCompound
import io.sc3.library.networking.NetworkUtil.sendToAllTracking
import io.sc3.peripherals.Registration.ModBlockEntities.printer
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import io.sc3.peripherals.prints.PrintData
import io.sc3.peripherals.prints.PrintItem
import io.sc3.peripherals.util.BaseBlockEntity
import io.sc3.peripherals.util.ImplementedInventory
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PrinterBlockEntity(
  pos: BlockPos,
  state: BlockState
) : BaseBlockEntity(printer, pos, state), NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {
  private val inventory = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY)

  /** Set of computers that are attached as a peripheral to the printer, so they may receive print state events. */
  val computers: MutableSet<IComputerAccess> = Collections.newSetFromMap(ConcurrentHashMap())

  var data: PrintData = PrintData()
  var printing = false
    set(value) {
      val oldValue = field
      field = value
      printProgress = 0

      if (!oldValue && value) {
        PrinterPeripheral.sendPrintStatusEvent(this)
      }
    }
  var printCount = 0
  private var outputStack: ItemStack = ItemStack.EMPTY

  var chamelium = 0
  var ink = 0
  var printProgress = 0
  // This property is synced from the server's config to the client
  var maxPrintProgress: Int = config.getOrElse("printer.print_ticks", 100)

  private var inksDirty = false
  private var outputDirty = false
  private var dataDirty = false

  // Renderer items for the client
  var previewData: PrintData? = null
    set(value) {
      field = value
      if (value != null) previewStack = PrintItem.create(value)
    }
  var previewStack: ItemStack = ItemStack.EMPTY

  private val propertyDelegate: PropertyDelegate = object : PropertyDelegate {
    override fun get(index: Int): Int {
      return when (index) {
        0 -> chamelium
        1 -> ink
        2 -> printProgress
        3 -> maxPrintProgress
        else -> 0
      }
    }

    override fun set(index: Int, value: Int) {
      when(index) {
        0 -> chamelium = value
        1 -> ink = value
        2 -> printProgress = value
        3 -> maxPrintProgress = value
      }
    }

    override fun size() = 4
  }

  override fun getItems(): DefaultedList<ItemStack> = inventory

  override fun onBroken() {
    super.onBroken()
    ItemScatterer.spawn(world, pos, inventory)
    inventory.clear()
  }

  override fun isValid(slot: Int, stack: ItemStack) = when(slot) {
    CHAMELIUM_SLOT -> chameliumValue(stack) > 0
    INK_SLOT -> stack.isOf(ModItems.inkCartridge)
    else -> false
  }

  override fun getAvailableSlots(side: Direction): IntArray = when(side) {
    Direction.DOWN -> downSideSlots
    else -> otherSideSlots
  }

  override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) =
    isValid(slot, stack)

  override fun canExtract(slot: Int, stack: ItemStack, dir: Direction) =
    !isValid(slot, stack) // Allow extracting output items from any direction

  private fun chameliumValue(stack: ItemStack): Int {
    if (stack.isOf(ModItems.chamelium)) {
      return chameliumValue
    } else if (stack.isOf(ModItems.print) && recycleMultiplier > 0) {
      val data = PrintItem.printData(stack)
      val cost = data.computeCosts()?.first ?: return 0
      return (cost * recycleMultiplier).toInt()
    } else {
      return 0
    }
  }

  private fun inkValue(stack: ItemStack) =
    if (stack.isOf(ModItems.inkCartridge)) inkValue else 0

  val canPrint
    get() = data.shapesOff.isNotEmpty() && data.shapesOff.size <= maxShapes && data.shapesOn.size <= maxShapes

  private fun canMergeOutput(): Boolean {
    val current = getStack(OUTPUT_SLOT)
    val output = PrintItem.create(data)
    return current.isEmpty || ItemStack.canCombine(current, output)
  }

  fun onTick(world: World) {
    if (world.isClient) return

    tickInputSlot()
    tickOutputSlot()

    if (inksDirty || outputDirty || dataDirty) {
      markDirty()
    }

    // Send ink update packets to any tracking entities
    if (inksDirty) {
      sendToAllTracking(world.getWorldChunk(pos), PrinterInkPacket(pos, chamelium, ink))
      inksDirty = false
    }

    if (outputDirty) {
      outputDirty = false
    }

    // Send data update packets to any tracking entities
    if (dataDirty) {
      sendToAllTracking(world.getWorldChunk(pos), PrinterDataPacket(pos, data))
      dataDirty = false
    }
  }

  private fun tickInputSlot() {
    // The value of one item in this slot. Don't allow any item waste. Only process one item per tick.
    val inputChamelium = chameliumValue(getStack(CHAMELIUM_SLOT))
    if (inputChamelium > 0 && maxChamelium - chamelium >= inputChamelium) {
      val stack = removeStack(CHAMELIUM_SLOT, 1)
      if (!stack.isEmpty) chamelium += inputChamelium
      inksDirty = true
    }

    val inputInk = inkValue(getStack(INK_SLOT))
    if (inputInk > 0 && maxInk - ink >= inputInk) {
      val stack = removeStack(INK_SLOT, 1)
      if (!stack.isEmpty) {
        ink += inputInk
        inksDirty = true

        // Replace the ink cartridge with an empty cartridge
        setStack(INK_SLOT, ItemStack(ModItems.emptyInkCartridge))
      }
    }
  }

  private fun tickOutputSlot() {
    // Printing logic
    if (printing && outputStack.isEmpty && canMergeOutput()) {
      val cost = data.computeCosts()
      if (cost != null) {
        val (chameliumCost, inkCost) = cost
        if (chamelium >= chameliumCost && ink >= inkCost) {
          // Start printing a single item and consume the inks
          chamelium -= chameliumCost
          ink -= inkCost
          inksDirty = true

          printCount--
          outputStack = PrintItem.create(data)
          if (printCount < 1) printing = false

          outputDirty = true
        }
      } else {
        printing = false
        outputDirty = true
        data = PrintData()
        dataUpdated()
      }
    }

    if (!outputStack.isEmpty) {
      printProgress = (printProgress + 1).coerceAtMost(maxPrintProgress)

      if (printProgress >= maxPrintProgress) {
        val result = getStack(OUTPUT_SLOT)
        if (result.isEmpty) {
          setStack(OUTPUT_SLOT, outputStack)
        } else if (result.count < result.maxCount && canMergeOutput()) {
          result.count++
        } else {
          return
        }

        printProgress = 0
        outputStack = ItemStack.EMPTY
        outputDirty = true

        PrinterPeripheral.sendPrintCompleteEvent(this)
        PrinterPeripheral.sendPrintStatusEvent(this)
      }
    }
  }

  override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler =
    PrinterScreenHandler(syncId, inv, this, propertyDelegate)

  override fun getDisplayName(): Text = Text.translatable(cachedState.block.translationKey)

  override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)

    Inventories.readNbt(nbt, inventory)

    data = PrintData.fromNbt(nbt.getCompound("data"))
    printing = nbt.getBoolean("printing")
    printCount = nbt.getInt("printCount")
    outputStack = nbt.optCompound("outputStack")?.let { ItemStack.fromNbt(it) } ?: ItemStack.EMPTY

    chamelium = nbt.getInt("chamelium")
    ink = nbt.getInt("ink")
    printProgress = nbt.getInt("printProgress")

    if (world?.isClient == true) previewData = data
  }

  override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)

    Inventories.writeNbt(nbt, inventory)

    nbt.put("data", data.toNbt())
    nbt.putBoolean("printing", printing)
    nbt.putInt("printCount", printCount)
    nbt.put("outputStack", outputStack.writeNbt(NbtCompound()))

    nbt.putInt("chamelium", chamelium)
    nbt.putInt("ink", ink)
    nbt.putInt("printProgress", printProgress)
  }

  override fun toUpdatePacket(): Packet<ClientPlayPacketListener> =
    BlockEntityUpdateS2CPacket.create(this)

  override fun toInitialChunkDataNbt(): NbtCompound {
    val nbt = super.toInitialChunkDataNbt()
    writeNbt(nbt)
    return nbt
  }

  override fun markDirty() {
    super<BaseBlockEntity>.markDirty()
  }

  fun dataUpdated() {
    val wasPrinting = printing
    printing = false
    dataDirty = true

    if (wasPrinting && !printing) {
      PrinterPeripheral.sendPrintStatusEvent(this)
    }
  }

  val peripheral by lazy { PrinterPeripheral(this) }

  companion object {
    const val maxChamelium = 256000
    const val maxInk = 100000

    val downSideSlots = intArrayOf(OUTPUT_SLOT, INK_SLOT) // allow extracting output prints and empty ink cartridges
    val otherSideSlots = intArrayOf(CHAMELIUM_SLOT, INK_SLOT)

    val chameliumValue: Int = config.get("printer.chamelium_value")
    val inkValue: Int = config.get("printer.ink_value")

    val recycleMultiplier: Double = config.get("printer.recycle_value_multiplier")

    val maxShapes: Int = config.get("printer.max_shapes")

    fun onTick(world: World, pos: BlockPos, state: BlockState, be: PrinterBlockEntity) {
      be.onTick(world)
    }
  }
}
