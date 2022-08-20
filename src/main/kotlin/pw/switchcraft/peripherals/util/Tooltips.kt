package pw.switchcraft.peripherals.util

import net.minecraft.text.Text
import net.minecraft.text.Text.translatable
import net.minecraft.util.Formatting.GRAY

object Tooltips {
  fun addDescLines(tooltip: MutableList<Text>, baseKey: String) {
    val baseText = translatable("$baseKey.desc")
    val lines = baseText.string.split("\n")
    lines.map { Text.literal(it.trim()).formatted(GRAY) }.toCollection(tooltip)
  }
}
