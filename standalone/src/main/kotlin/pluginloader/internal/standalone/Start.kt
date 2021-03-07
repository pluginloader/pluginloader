package pluginloader.internal.standalone

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pluginloader.api.caching
import pluginloader.api.nonNull
import pluginloader.internal.shared.InternalPlugin
import pluginloader.internal.shared.PluginController
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val controller = PluginController(::load, {_, name, _ -> Plugin(name)})
val configFile = File("config.json")
val json = Json{prettyPrint = true}
private lateinit var config: Config
val mainThread: ExecutorService = Executors.newSingleThreadExecutor()

private fun saveConfig(){
    configFile.writeText(json.encodeToString(Config.serializer(), config))
}

fun inMain(invoke: () -> Unit){
    mainThread.submit{caching(invoke).nonNull(Throwable::printStackTrace)}
}

fun main() {
    inMain{
        if(configFile.exists()) {
            config = json.decodeFromString(Config.serializer(), configFile.readText())
        }else {
            config = Config()
            config.pluginDirs.forEach{File(it).mkdir()}
            configFile.createNewFile()
            configFile.writeText(json.encodeToString(Config.serializer(), config))
        }
        controller.cache = config.cache
        controller.mavenCache = config.mavenCache
        config.plugins.forEach{ name ->
            if(controller.exists(name))return@forEach
            val jar = getFile(name) ?: return@forEach
            caching {
                controller.load(jar)
            }.nonNull(Throwable::printStackTrace)
        }
    }
    caching{System.`in`.bufferedReader().use {
        while (true) {
            val line = it.readLine() ?: break
            if(line == "exit")break
            inMain{
                val args = line.split(" ")
                when (args[0].toLowerCase()) {
                    "plu", "pluginloader" -> {
                        if (args.size == 1) {
                            println("Usage: pluginloader [load|unload|show|reloadcfg]")
                            return@inMain
                        }
                        when (args[1].toLowerCase()) {
                            "l", "load" -> {
                                if (args.size == 2) {
                                    println("Usage: pluginloader load [Plugin]")
                                    return@inMain
                                }
                                val name = args[2]
                                if (controller.exists(name)) {
                                    controller.reload(name)
                                    println("$name reloaded")
                                    return@inMain
                                }
                                val file = getFile(name)
                                if (file == null || !file.exists()) {
                                    println("$name not found")
                                    return@inMain
                                }
                                config.plugins.add(name)
                                saveConfig()
                                controller.load(file)
                                println("$name loaded")
                            }
                            "u", "unload" -> {
                                if (args.size == 2) {
                                    println("Usage: pluginloader unload [Plugin]")
                                    return@inMain
                                }
                                val name = args[2]
                                var exists = config.plugins.remove(name)
                                if(exists) saveConfig()
                                exists = controller.remove(name) || exists
                                println(if (exists) "$name unloaded" else "$name not found")
                            }
                            "s", "show" -> {
                                controller.forEach { name, dependency ->
                                    println("Plugin: $name${
                                        if (dependency.isNotEmpty())
                                            ", dependency: $dependency" else ""
                                    }")
                                }
                            }
                            "r", "reloadcfg" -> {
                                config = json.decodeFromString(Config.serializer(), configFile.readText())
                                controller.cache = config.cache
                                controller.mavenCache = config.mavenCache
                            }
                        }
                    }
                    else -> {
                        println("Unknown command")
                    }
                }
            }
        }
    }}.nonNull(Throwable::printStackTrace)
    controller.unloadAll()
    mainThread.shutdown()
}

fun load(name: String){
    val file = getFile(name) ?: return
    controller.load(file)
}

fun getFile(name: String): File? {
    config.pluginDirs.forEach{
        val file = File("$it/$name.jar")
        if(file.exists())return file
    }
    return null
}

class Plugin(name: String): InternalPlugin(controller, name){
    override fun task(task: () -> Unit) {
        mainThread.submit(task)
    }
}

@Serializable
private data class Config(
    val pluginDirs: MutableList<String> = arrayListOf("plugins"),
    val plugins: MutableList<String> = ArrayList(),
    val cache: String = "plu_cache",
    val mavenCache: String = "plu_cache/maven"
)