package pluginloader.api

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

interface LoaderPlugin {
    val name: String

    fun load(kClass: KClass<*>)

    fun unloadHandler(handler: () -> Unit)

    fun removeUnloadHandler(handler: () -> Unit)

    fun task(task: () -> Unit)

    fun <T: Annotation, Obj> fieldReplacer(kClass: KClass<T>, handler: (plugin: LoaderPlugin, annotation: T, input: Obj) -> Obj)

    @Deprecated("Use fieldReplacer")
    fun <T: Annotation> fieldHandler(kClass: KClass<T>, handler: (Field, T, LoaderPlugin) -> Unit)

    fun <T: Annotation> methodHandler(kClass: KClass<T>, handler: (Method, T, LoaderPlugin) -> Unit)
}

inline fun <T: Annotation, Obj> LoaderPlugin.fieldReplacer(kClass: KClass<T>, obj: Obj){
    fieldReplacer<T, Obj>(kClass){_, _, _ -> obj}
}

inline fun <T: Annotation, Obj> LoaderPlugin.fieldReplacer(kClass: KClass<T>, crossinline handler: (plugin: LoaderPlugin) -> Obj){
    fieldReplacer<T, Obj>(kClass){plugin, _, _ -> handler(plugin)}
}