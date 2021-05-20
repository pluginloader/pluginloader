package pluginloader.internal.standalone

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pluginloader.api.Args
import pluginloader.api.CmdSender
import pluginloader.api.caching
import pluginloader.api.nonNull
import pluginloader.internal.shared.InternalPlugin
import pluginloader.internal.shared.PluginController
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val controller = PluginController(::load, {_, name, _ -> Plugin(name)})
private val configFile = File("config.json")
private val commandMapping = HashMap<String, (CmdSender, Args) -> Unit>()
private val json = Json{prettyPrint = true}
private lateinit var config: Config
private val mainThread: ExecutorService = Executors.newSingleThreadExecutor()
private val sender = object: CmdSender{
    override fun sendMessage(string: String) {
        println(string)
    }
}

private fun saveConfig(){
    configFile.writeText(json.encodeToString(Config.serializer(), config))
}

private fun inMain(invoke: () -> Unit){
    mainThread.submit{caching(invoke).nonNull(Throwable::printStackTrace)}
}

internal fun main() {
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
        fun cmd(sender: CmdSender, args: Args){
            if (args.isEmpty()) {
                println("Usage: pluginloader [load|unload|show|reloadcfg]")
                return
            }
            when (args[0].lowercase()) {
                "l", "load" -> {
                    if (args.size == 1) {
                        sender.sendMessage("Usage: pluginloader load [Plugin]")
                        return
                    }
                    val name = args[1]
                    if (controller.exists(name)) {
                        controller.reload(name)
                        sender.sendMessage("$name reloaded")
                        return
                    }
                    val file = getFile(name)
                    if (file == null || !file.exists()) {
                        sender.sendMessage("$name not found")
                        return
                    }
                    config.plugins.add(name)
                    saveConfig()
                    controller.load(file)
                    sender.sendMessage("$name loaded")
                }
                "u", "unload" -> {
                    if (args.size == 1) {
                        sender.sendMessage("Usage: pluginloader unload [Plugin]")
                        return
                    }
                    val name = args[1]
                    var exists = config.plugins.remove(name)
                    if(exists) saveConfig()
                    exists = controller.remove(name) || exists
                    sender.sendMessage(if (exists) "$name unloaded" else "$name not found")
                }
                "s", "show" -> {
                    controller.forEach { name, dependency ->
                        sender.sendMessage("Plugin: $name${
                            if (dependency.isNotEmpty())
                                ", dependency: $dependency" else ""
                        }")
                    }
                }
                "r", "reloadcfg" -> {
                    config = json.decodeFromString(Config.serializer(), configFile.readText())
                    controller.cache = config.cache
                    controller.mavenCache = config.mavenCache
                    sender.sendMessage("Reloaded config")
                }
                else -> {
                    println("Usage: pluginloader [load|unload|show|reloadcfg]")
                }
            }
        }
        commandMapping["plu"] = ::cmd
        commandMapping["pluginloader"] = ::cmd
    }
    caching{System.`in`.bufferedReader().use {
        while (true) {
            val line = it.readLine() ?: break
            if(line == "exit" || line == "stop")break
            inMain{
                val args = line.split(" ")
                val command = commandMapping[args[0].lowercase()]
                if(command == null){
                    println("Unknown command")
                    return@inMain
                }
                command(sender, args.drop(1).toTypedArray())
            }
        }
    }}.nonNull(Throwable::printStackTrace)
    println("Stopping")
    controller.unloadAll()
    mainThread.shutdown()
}

private fun load(name: String){
    val file = getFile(name) ?: return
    controller.load(file)
}

private fun getFile(name: String): File? {
    config.pluginDirs.forEach{
        val file = File("$it/$name.jar")
        if(file.exists())return file
    }
    return null
}

private class Plugin(name: String): InternalPlugin(controller, name){
    override fun task(task: () -> Unit) {
        mainThread.submit(task)
    }

    override fun cmd(command: String, cmd: (CmdSender, Args) -> Unit, vararg aliases: String) {
        aliases.forEach{commandMapping[it.lowercase()] = cmd}
        unloadHandler{aliases.forEach{commandMapping.remove(it.lowercase())}}
    }
}

@Serializable
private data class Config(
    val pluginDirs: MutableList<String> = arrayListOf("plugins"),
    val plugins: MutableList<String> = ArrayList(),
    val cache: String = "plu_cache",
    val mavenCache: String = "plu_cache/maven"
)