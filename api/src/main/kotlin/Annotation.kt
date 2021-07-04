package pluginloader.api

//@Load internal fun load(plugin: LoaderPlugin){}
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Load

//@Unload internal fun unload(){}
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unload