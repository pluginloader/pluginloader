package pluginloader.internal.shared

import pluginloader.api.nonNull
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

internal class JarPlugin(private val resolve: (name: String, List<String>) -> Class<out Any>?,
                         private val dependencyLoad: (List<String>) -> Unit,
                         private val exceptions: (Throwable) -> Unit,
                         private val jar: File, private val plugin: InternalPlugin){
    var loaded = false
    val dependency: MutableList<String> = ArrayList()
    val initDependency: MutableList<String> = ArrayList()
    private val classes = HashSet<String>()
    private var loader: PluginClassLoader? = null

    fun clazz(name: String): Class<out Any>?{
        if(!classes.contains(name))return null
        return (loader ?: return null).loadClass(name)
    }

    fun unload(){
        plugin.unload()
        dependency.clear()
        classes.clear()
        loader = null
        loaded = false
    }

    fun load(){
        loaded = true
        val get: (String) -> ByteArray?
        val getFiles: (String) -> ByteArray?
        val mainClasses = ArrayList<String>()
        if(jar.length() > 300 * 1024) {//300 kb
            val zip = ZipFile(jar)
            zip.size()
            zip.entries().iterator().forEach{
                val name = it.name
                if(name.endsWith(".class")) {
                    val normalized = normalizeClassName(name)
                    classes.add(normalized)
                    if(normalized.endsWith("PluginKt")) mainClasses.add(normalized)
                } else if(name.endsWith(".dependency")){
                    val index = name.lastIndexOf('/') + 1
                    val entry = zip.getEntry(name)
                    val pluginName = name.replace(".dependency", "").substring(index)
                    if(!plugin.controller.exists(pluginName)) {
                        val input = String(zip.getInputStream(entry).readBytes())
                        if (input.isNotEmpty()) {
                            val split = input.split(":")
                            PluginRepo.official.get(File(plugin.controller.cache, "repo"), split[0], split[1]).nonNull{newJar ->
                                plugin.controller.load(newJar, pluginName)
                            }
                        }
                    }
                    dependency.add(pluginName)
                } else if(name.endsWith(".mavenDependency")){
                    val split = name.substring(name.lastIndexOf('/') + 1).replace(".mavenDependency", "").split(";")
                    plugin.controller.loadMaven(split[0], split[1], split[2])
                    dependency.add(split[1])
                }
            }
            get = get@ {
                if(!classes.contains(it))return@get null
                val entry = zip.getEntry(it.replace(".", "/") + ".class")
                zip.getInputStream(entry).use{input -> return@get input.readBytes()}
            }
            getFiles = get@ {
                zip.getInputStream(zip.getEntry(it)).use{input -> return@get input.readBytes()}
            }
            plugin.postUnload(zip::close)
        }else {
            val loadedJar = jar.readBytes()
            val inStream = ZipInputStream(ByteArrayInputStream(loadedJar))
            val files = HashMap<String, ByteArray>()
            while (true) {
                val entry = inStream.nextEntry ?: break
                val name = entry.name
                val bytes = inStream.readBytes()
                if (name.endsWith(".class")) {
                    val normalized = normalizeClassName(name)
                    files[normalized] = bytes
                    classes.add(normalized)
                    if (normalized.endsWith("PluginKt")) mainClasses.add(normalized)
                } else if (name.endsWith(".dependency")) {
                    val index = name.lastIndexOf('/') + 1
                    val pluginName = name.replace(".dependency", "").substring(index)
                    if(!plugin.controller.exists(pluginName)) {
                        val input = String(bytes)
                        if (input.isNotEmpty()) {
                            val split = input.split(":")
                            PluginRepo.official.get(File(plugin.controller.cache, "repo"), split[0], split[1]).nonNull{newJar ->
                                plugin.controller.load(newJar, pluginName)
                            }
                        }
                    }
                    dependency.add(pluginName)
                } else if(name.endsWith(".mavenDependency")){
                    val split = name.substring(name.lastIndexOf('/') + 1).replace(".mavenDependency", "").split(";")
                    plugin.controller.loadMaven(split[0], split[1], split[2])
                    dependency.add(split[1])
                } else {
                    files[name] = bytes
                }
                inStream.closeEntry()
            }
            get = files::remove
            getFiles = files::remove
        }

        dependency.addAll(initDependency)
        dependencyLoad(dependency)
        loader = PluginClassLoader(getFiles, get){resolve(it, dependency)}
        plugin.startLoad()
        mainClasses.forEach{
            try{
                plugin.mayExceptionLoad(loader!!.loadClass(it).kotlin).nonNull(exceptions)
            }catch (th: Throwable){
                exceptions(th)
            }
        }
    }

    private fun normalizeClassName(name: String) = name.replace("/", ".").replace(".class", "")
}

private class PluginClassLoader(
        private val files: (String) -> ByteArray?,
        private val classes: (String) -> ByteArray?,
        private val dependency: (String) -> Class<out Any>?):
        ClassLoader(PluginClassLoader::class.java.classLoader){
    override fun findClass(clazz: String): Class<*> {
        val bytes = classes(clazz) ?: return dependency(clazz) ?: throw ClassNotFoundException(clazz)
        return defineClass(clazz, bytes, 0, bytes.size)
    }

    override fun getResourceAsStream(name: String): InputStream? {
        val remove = files(name) ?: return null
        return ByteArrayInputStream(remove)
    }
}