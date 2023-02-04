package io.sc3.peripherals.posters

import io.sc3.library.ext.optString
import io.sc3.library.ext.putOptString
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import io.sc3.peripherals.mixin.MapColorAccessor
import io.sc3.peripherals.posters.PosterItem.Companion.POSTER_KEY
import net.minecraft.block.MapColor
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import kotlin.math.roundToInt
import kotlin.math.sign

const val MAX_LABEL_LENGTH = 48
const val MAX_TOOLTIP_LENGTH = 256

fun getDefaultPalette() = MapColorAccessor.getColors().map { it?.color ?: MapColor.CLEAR.color }.toIntArray()

data class PosterPrintData(
  private val initialLabel: String? = null,
  var tooltip: String? = null,
  val colors: ByteArray = ByteArray(128 * 128),
  var palette: IntArray = getDefaultPalette(),
  var posterId: String? = null,
) {
  var label: String? = initialLabel
    set(value) {
      field = value?.takeIf { it.isValidLabel() }
      labelText = field?.let { Text.of(it) }
    }

  var labelText: Text? = initialLabel?.let { Text.of(it) }
    private set

  fun computeCosts(): Int {
    val pixels = colors.count { it != 0.toByte() }
    return (posterInkCost*(pixels / 16384.0)).roundToInt().coerceAtLeast(pixels.sign)
  }

  fun toNbt() = toItemNbt().apply {
    putByteArray("colors", colors)
    putIntArray("palette", palette)
  }

  fun toItemNbt() = NbtCompound().apply {
    putOptString("label", label?.takeIf { it.isValidLabel() })
    putOptString("tooltip", tooltip?.takeIf { it.isValidTooltip() })
    putOptString(POSTER_KEY, posterId)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PosterPrintData

    if (tooltip != other.tooltip) return false
    if (!colors.contentEquals(other.colors)) return false
    if (!palette.contentEquals(other.palette)) return false
    if (posterId != other.posterId) return false
    if (label != other.label) return false

    return true
  }

  override fun hashCode(): Int {
    var result = tooltip?.hashCode() ?: 0
    result = 31 * result + colors.contentHashCode()
    result = 31 * result + palette.contentHashCode()
    result = 31 * result + (posterId?.hashCode() ?: 0)
    result = 31 * result + (label?.hashCode() ?: 0)
    return result
  }

  companion object {
    val posterInkCost: Int = config.get("poster_printer.ink_cost")

    fun fromNbt(nbt: NbtCompound) = PosterPrintData(
      initialLabel = nbt.optString("label")?.takeIf { it.isValidLabel() },
      tooltip = nbt.optString("tooltip"),
      colors = nbt.getByteArray("colors").takeIf { it.size == 16384 } ?: ByteArray(128 * 128),
      palette = nbt.getIntArray("palette").takeIf { it.size == 64 } ?: getDefaultPalette(),
      posterId = nbt.optString(POSTER_KEY)?.ifEmpty { null },
    )

    private fun String?.isValidLabel() = this?.length in 1..MAX_LABEL_LENGTH
    private fun String?.isValidTooltip() = this?.length in 1..MAX_TOOLTIP_LENGTH
  }
}
