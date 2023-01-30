package io.sc3.peripherals.posters.printer

import dan200.computercraft.api.peripheral.IComputerAccess
import io.sc3.library.ext.optCompound
import io.sc3.library.networking.NetworkUtil.sendToAllTracking
import io.sc3.peripherals.Registration.ModBlockEntities.posterPrinter
import io.sc3.peripherals.Registration.ModItems
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import io.sc3.peripherals.posters.PosterItem
import io.sc3.peripherals.posters.PosterItem.Companion.POSTER_KEY
import io.sc3.peripherals.posters.PosterPrintData
import io.sc3.peripherals.util.BaseBlockEntity
import io.sc3.peripherals.util.ImplementedInventory
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerChunkManager
import net.minecraft.text.Text
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PosterPrinterBlockEntity(
  pos: BlockPos,
  state: BlockState
) : BaseBlockEntity(posterPrinter, pos, state), NamedScreenHandlerFactory, ImplementedInventory, SidedInventory {
  private val inventory = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY)

  val log = LoggerFactory.getLogger("ScPeripherals/PosterPrinterBlockEntity")!!

  /** Set of computers that are attached as a peripheral to the printer, so they may receive print state events. */
  val computers: MutableSet<IComputerAccess> = Collections.newSetFromMap(ConcurrentHashMap())

  var animatingPosterId: String? = null
  var animationStartTime: Long = 0

  var data: PosterPrintData = PosterPrintData(
    null, null, MutableList(128*128) { 0 }
  )
  var printing = false
    set(value) {
      val oldValue = field
      field = value
      printProgress = 0

      if (!oldValue && value) {
        PosterPrinterPeripheral.sendPrintStatusEvent(this)
      }
    }
  var printCount = 0
  private var outputStack: ItemStack = ItemStack.EMPTY

  var ink = 0
  var printProgress = 0
  // This property is synced from the server's config to the client
  var maxPrintProgress: Int = config.getOrElse("printer.print_ticks", 100)

  private var inksDirty = false
  private var outputDirty = false
  private var dataDirty = false

  private val propertyDelegate: PropertyDelegate = object : PropertyDelegate {
    override fun get(index: Int): Int {
      return when (index) {
        1 -> ink
        2 -> printProgress
        3 -> maxPrintProgress
        else -> 0
      }
    }

    override fun set(index: Int, value: Int) {
      when(index) {
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
    PAPER_SLOT -> stack.isOf(Items.PAPER)
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

  private fun inkValue(stack: ItemStack) =
    if (stack.isOf(ModItems.inkCartridge)) inkValue else 0

  fun canMergeOutput(): Boolean {
    val current = getStack(OUTPUT_SLOT)
    val output = PosterItem.create(world ?: return false, data)

    return current.isEmpty || (current.isItemEqual(output) && ItemStack.areNbtEqual(current, output))
  }

  fun onTick(world: World) {
    if (world.isClient) return

    tickInputSlot()
    tickOutputSlot(world)

    if (inksDirty || outputDirty || dataDirty) {
      markDirty()
    }

    if (outputDirty || world.time % 20 == 0L) {
      PosterItem.getPosterId(getStack(OUTPUT_SLOT))?.let { sendPosterState(it, world) }
    }

    // Send ink update packets to any tracking entities
    if (inksDirty) {
      sendToAllTracking(world.getWorldChunk(pos), PosterPrinterInkPacket(pos, ink))
      inksDirty = false
    }

    if (outputDirty) {
      outputDirty = false
    }

    // Send data update packets to any tracking entities
    if (dataDirty) {
//      sendToAllTracking(world.getWorldChunk(pos), PrinterDataPacket(pos, data))
      dataDirty = false
    }

    val activelyPrinting = !outputStack.isEmpty
    if (activelyPrinting != cachedState.get(PosterPrinterBlock.printing)) {
      world.setBlockState(pos, cachedState.with(PosterPrinterBlock.printing, activelyPrinting))
    }

    val hasPaper = !getStack(PAPER_SLOT).isEmpty
    if (hasPaper != cachedState.get(PosterPrinterBlock.hasPaper)) {
      world.setBlockState(pos, cachedState.with(PosterPrinterBlock.hasPaper, hasPaper))
    }
  }

  private fun sendPosterState(posterId: String, world: World) {
    val state = PosterItem.getPosterState(posterId, world)
    if (state != null) {
      val chunk = world.getWorldChunk(pos)
      val storage = (chunk.world.chunkManager as ServerChunkManager).threadedAnvilChunkStorage
      storage.getPlayersWatchingChunk(chunk.pos, false).forEach {
        state.update(it)
        val packet = state.getPlayerUpdatePacket(posterId, it)
        if (packet != null) {
          it.networkHandler.sendPacket(packet.toS2CPacket())
        }
      }
    }
  }

  private fun tickInputSlot() {
    // The value of one item in this slot. Don't allow any item waste. Only process one item per tick.

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

  private fun tickOutputSlot(world: World) {
    // Printing logic
    if (printing && outputStack.isEmpty && canMergeOutput()) {
      val cost = data.computeCosts()
      if (cost != null) {
        val paperStack = getStack(PAPER_SLOT)
        if (ink >= cost && paperStack.count >= 1) {
          // Start printing a single item and consume the inks
          ink -= cost
          inksDirty = true
          paperStack.decrement(1)

          printCount--
          outputStack = PosterItem.create(world, data)
          val posterId = outputStack.nbt!!.getString(POSTER_KEY)
          data.posterId = posterId // Allow merging with the output stack
          if (printCount < 1) printing = false

          // Send animation packet to all tracking entities
          sendToAllTracking(world.getWorldChunk(pos), PosterPrinterStartPrintPacket(pos, posterId))
          sendPosterState(posterId, world) // Make sure clients can render the poster during the print animation

          outputDirty = true
        }
      } else {
        printing = false
        outputDirty = true
        data = PosterPrintData()
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

        PosterPrinterPeripheral.sendPrintCompleteEvent(this)
        PosterPrinterPeripheral.sendPrintStatusEvent(this)
      }
    }
  }

  override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler =
    PosterPrinterScreenHandler(syncId, inv, this, propertyDelegate)

  override fun getDisplayName(): Text = Text.translatable(cachedState.block.translationKey)

  override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)

    inventory.clear()
    Inventories.readNbt(nbt, inventory)

    data = PosterPrintData.fromNbt(nbt.getCompound("data"))
    printing = nbt.getBoolean("printing")
    printCount = nbt.getInt("printCount")
    outputStack = nbt.optCompound("outputStack")?.let { ItemStack.fromNbt(it) } ?: ItemStack.EMPTY

    ink = nbt.getInt("ink")
    printProgress = nbt.getInt("printProgress")
  }

  override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)

    Inventories.writeNbt(nbt, inventory)

    nbt.put("data", data.toNbt())
    nbt.putBoolean("printing", printing)
    nbt.putInt("printCount", printCount)
    nbt.put("outputStack", outputStack.writeNbt(NbtCompound()))

    nbt.putInt("ink", ink)
    nbt.putInt("printProgress", printProgress)
  }

  override fun toUpdatePacket(): Packet<ClientPlayPacketListener> =
    BlockEntityUpdateS2CPacket.create(this)

  override fun toInitialChunkDataNbt(): NbtCompound {
    val nbt = super.toInitialChunkDataNbt()
    writeNbt(nbt)
    nbt.remove("data") // Don't send the print data to the client

    return nbt
  }

  override fun markDirty() {
    super<BaseBlockEntity>.markDirty()
    getWorld()!!.updateListeners(getPos(), cachedState, cachedState, Block.NOTIFY_ALL)
  }

  fun dataUpdated() {
    val wasPrinting = printing
    printing = false
    dataDirty = true

    if (wasPrinting && !printing) {
      PosterPrinterPeripheral.sendPrintStatusEvent(this)
    }
  }

  val peripheral by lazy { PosterPrinterPeripheral(this) }

  companion object {
    const val maxInk = 100000

    val downSideSlots = intArrayOf(OUTPUT_SLOT, INK_SLOT) // allow extracting output prints and empty ink cartridges
    val otherSideSlots = intArrayOf(PAPER_SLOT, INK_SLOT)

    val inkValue: Int = config.get("printer.ink_value")

    fun onTick(world: World, pos: BlockPos, state: BlockState, be: PosterPrinterBlockEntity) {
      be.onTick(world)
    }
  }
}
