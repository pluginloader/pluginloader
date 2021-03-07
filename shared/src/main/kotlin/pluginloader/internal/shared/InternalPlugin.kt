package pluginloader.internal.shared

import pluginloader.api.LoaderPlugin
import pluginloader.api.caching
import pluginloader.api.nonNull
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

abstract class InternalPlugin(val controller: PluginController, override val name: String): LoaderPlugin {
    private val unloadHandlers = ArrayList<() -> Unit>()

    override fun unloadHandler(handler: () -> Unit) {
        unloadHandlers.add(handler)
    }

    override fun removeUnloadHandler(handler: () -> Unit) {
        unloadHandlers.remove(handler)
    }

    open fun startLoad(){}

    open fun unload() {
        unloadHandlers.forEach {
            try {
                it()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        unloadHandlers.clear()
    }

    fun mayExceptionLoad(kClass: KClass<*>): Throwable? {
        return caching {
            val java = kClass.java
            java.declaredFields.forEach fields@{
                if (!Modifier.isStatic(it.modifiers)) return@fields
                it.isAccessible = true
                it.declaredAnnotations.forEach annotation@{ annotation ->
                    controller.initField(it, annotation, this)
                }
            }
            java.declaredMethods.forEach method@{
                if (!Modifier.isStatic(it.modifiers)) return@method
                it.isAccessible = true
                it.declaredAnnotations.forEach annotation@{ annotation ->
                    controller.initMethod(it, annotation, this)
                }
            }
        }
    }

    override fun load(kClass: KClass<*>) {
        mayExceptionLoad(kClass).nonNull { it.printStackTrace() }
    }

    override fun <T: Annotation, Obj> fieldReplacer(
        kClass: KClass<T>,
        handler: (plugin: LoaderPlugin, annotation: T, input: Obj) -> Obj
    ) {
        fieldHandler(kClass) { field, annotation, plugin ->
            field.unsetFinal()
            val result = field.get(null) as Obj
            @Suppress("UNCHECKED_CAST")
            field.set(null, handler(plugin, annotation, result))
        }
    }

    override fun <T : Annotation> fieldHandler(kClass: KClass<T>, handler: (Field, T, LoaderPlugin) -> Unit) {
        controller.fieldHandler(kClass, handler)
        unloadHandler { controller.unregisterFieldHandler(kClass) }
    }

    override fun <T : Annotation> methodHandler(kClass: KClass<T>, handler: (Method, T, LoaderPlugin) -> Unit) {
        controller.methodHandler(kClass, handler)
        unloadHandler { controller.unregisterMethodHandler(kClass) }
    }
}