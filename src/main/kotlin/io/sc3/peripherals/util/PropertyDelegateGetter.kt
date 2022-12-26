package io.sc3.peripherals.util

import net.minecraft.screen.PropertyDelegate
import kotlin.reflect.KProperty

class PropertyDelegateGetter(val delegate: PropertyDelegate, val index: Int = 0) {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): Int =
    delegate.get(index)
}
