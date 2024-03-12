package io.sc3.peripherals.util

import net.minecraft.screen.PropertyDelegate
import kotlin.reflect.KProperty

class PropertyDelegateGetter(private val delegate: PropertyDelegate, private val index: Int = 0) {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
    delegate.get(index)
}
