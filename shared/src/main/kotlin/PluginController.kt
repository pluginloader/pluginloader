package pluginloader.internal.shared

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pluginloader.api.Cmd
import pluginloader.api.Load
import pluginloader.api.LoaderPlugin
import pluginloader.api.Unload
import java.io.File
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

class PluginController(private val loadDependency: (String) -> Unit,
                       private val createPlugin: (controller: PluginController, name: String, path: File) -> InternalPlugin,
                       private val onException: (LoaderPlugin, Throwable) -> Unit = {plugin, ex ->
                           System.err.println("Plugin ${plugin.name} exception:")
                           ex.printStackTrace()
                       }){
    var mavenCache = "plu_cache/maven"
    var cache = "plu_cache"
    private val fields = HashMap<KClass<out Any>, (Field, Annotation, LoaderPlugin) -> Unit>()
    private val methods = HashMap<KClass<out Any>, (Method, Annotation, LoaderPlugin) -> Unit>()
    private val jarPlugins = HashMap<String, JarPlugin>()

    init{
        methodHandler(Load::class){ method, _, pl ->
            if(method.parameterCount == 1)method.invoke(null, pl)
            else method.invoke(null)
        }
        methodHandler(Unload::class){ method, _, pl -> pl.unloadHandler{method.invoke(null)}}
        methodHandler(Cmd::class){method, cmd, pl ->
            val handle = MethodHandles.lookup().unreflect(method)
            if(method.parameterCount == 1){
                pl.cmd(cmd.command, {sender, _ -> handle.invokeWithArguments(sender)}, *cmd.aliases)
            }else {
                pl.cmd(cmd.command, {sender, args -> handle.invokeWithArguments(sender, args)}, *cmd.aliases)
            }
        }
    }

    fun <T: Annotation> fieldHandler(kClass: KClass<T>, handler: (Field, T, LoaderPlugin) -> Unit){
        @Suppress("UNCHECKED_CAST")
        fields[kClass] = {a, b, c -> handler(a, b as T, c)}
    }

    fun <T: Annotation> unregisterFieldHandler(kClass: KClass<T>) = fields.remove(kClass)

    fun <T: Annotation> methodHandler(kClass: KClass<T>, handler: (Method, T, LoaderPlugin) -> Unit){
        @Suppress("UNCHECKED_CAST")
        methods[kClass] = {a, b, c -> handler(a, b as T, c)}
    }

    fun <T: Annotation> unregisterMethodHandler(kClass: KClass<T>) = methods.remove(kClass)

    fun initField(field: Field, annotation: Annotation, plugin: LoaderPlugin){
        (fields[annotation.annotationClass] ?: return).invoke(field, annotation, plugin)
    }

    fun initMethod(method: Method, annotation: Annotation, plugin: LoaderPlugin){
        (methods[annotation.annotationClass] ?: return).invoke(method, annotation, plugin)
    }

    fun exists(name: String) = jarPlugins.containsKey(name)

    fun forEach(fallback: (String, List<String>) -> Unit) =
            jarPlugins.forEach{ fallback(it.key, it.value.dependency) }

    fun load(jar: File, plName: String = jar.name.replace(".jar", ""), dep: List<String> = emptyList()){
        if(exists(plName))return
        val loadedPlugin = createPlugin(this, plName, jar)
        val loading = JarPlugin(rtn@{ name, dependency ->
            jarPlugins.forEach {
                if (dependency.contains(it.key)) {
                    val clazz = it.value.clazz(name)
                    if (clazz != null) return@rtn clazz
                }
            }
            return@rtn null
        }, {
            it.forEach { name ->
                val plu = jarPlugins[name]
                if(plu != null){
                    if(plu.loaded)return@forEach
                    plu.load()
                    return@forEach
                }
                loadDependency(name)
            }
        }, {
           onException(loadedPlugin, it)
        }, jar, loadedPlugin)
        loading.initDependency.addAll(dep)
        jarPlugins[plName] = loading
        loading.load()
    }

    fun loadMaven(group: String, artifact: String, version: String){
        if(exists(artifact))return
        Maven.download(Maven.central, mavenCache, group, artifact, version)
        val depFile = File(mavenCache, "${group.replace(".", "/")}/$artifact/$version-dependency.json")
        if(!depFile.exists())return
        val dependency = Json.decodeFromString(ListSerializer(MavenArtifactDependency.serializer()), depFile.readText())
        dependency.forEach{loadMaven(it.group, it.artifact, it.version!!)}
        load(File(mavenCache, "${group.replace(".", "/")}/$artifact/$version.jar"), artifact, dependency.map{it.artifact})
    }

    fun unloadAll(){
        jarPlugins.values.forEach {
            if(it.loaded)it.unload()
        }
    }

    fun remove(name: String): Boolean{
        val jar = jarPlugins[name] ?: return false
        unload(name, jar).forEach{
            if(!it.second.loaded)return@forEach
            jarPlugins.remove(it.first)
            it.second.unload()
        }
        return true
    }

    fun reload(name: String): Boolean{
        val jar = jarPlugins[name] ?: return false
        val list = unload(name, jar)
        list.forEach{
            if(!it.second.loaded)return@forEach
            it.second.unload()
        }
        list.forEach{
            if(it.second.loaded)return@forEach
            it.second.load()
        }
        return true
    }

    private fun unload(name: String, jar: JarPlugin): List<Pair<String, JarPlugin>>{
        val list = ArrayList<Pair<String, JarPlugin>>()
        fun recurciveReload(name: String, jar: JarPlugin){
            if(!jar.loaded)return
            val pair = Pair(name, jar)
            if(list.remove(pair)){
                list.add(0, pair)
            }else {
                list.add(pair)
            }
            jarPlugins.forEach{if(it.value.dependency.contains(name))recurciveReload(it.key, it.value)}
        }
        recurciveReload(name, jar)
        val newList = ArrayList<Pair<String, JarPlugin>>()
        list.forEach{input ->
            newList.forEachIndexed{i, item ->
                if(item.second.dependency.contains(input.first)){
                    newList.add(i, input)
                    return@forEach
                }
            }
            newList.add(input)
        }
        newList.reverse()
        return newList
    }
}