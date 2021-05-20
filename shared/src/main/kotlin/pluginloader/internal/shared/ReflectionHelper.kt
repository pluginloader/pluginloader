package pluginloader.internal.shared

import java.lang.reflect.Field
import java.lang.reflect.Modifier

private val modifiersField by lazy {
    val field = Field::class.java.getDeclaredField("modifiers")
    if(!field.isAccessible) field.isAccessible = true
    field
}

fun Field.unsetFinal(): Field {
    if(modifiers and Modifier.FINAL != 0)modifiersField.setInt(this, modifiers and Modifier.FINAL.inv())
    isAccessible = true
    return this
}