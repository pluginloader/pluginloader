package pluginloader.internal.bukkit.plugin

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import pluginloader.api.onlinePlayers
import pluginloader.api.plugin
import pluginloader.api.string
import pluginloader.internal.bukkit.shared.commandMap
import pluginloader.internal.bukkit.shared.register
import pluginloader.internal.shared.PluginController
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

class JavaPlugin: JavaPlugin(){
    private val configFile2 by lazy{File(file.parentFile, "plugins/config.yml")}
    private val config = YamlConfiguration()
    private val repos = ArrayList<String>()
    private val pluginsList = ArrayList<String>()
    private val controller = PluginController(this::load, ::Plugin){plu, ex ->
        val message = "Plugin ${plu.name} exception:\n${ex.stackTraceToString()}"
        onlinePlayers.forEach{if(it.isOp)it.sendMessage(message)}
        System.err.println("Plugin ${plu.name}, exception:")
        ex.printStackTrace()
    }

    override fun onEnable() {
        plugin = this
        val time = measureTimeMillis {
            register(controller)
            val pluginsDir = File(file.parentFile, "plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdir()
            if (configFile2.exists()) config.load(configFile2)
            else defaultConfig()
            controller.cache = config.string("cache", controller.cache)
            controller.mavenCache = config.string("mavenCache", controller.mavenCache)
            repos.addAll(config.getStringList("repos"))
            config.getStringList("plugins").forEach(this::loadFromRepos)
            config.save(configFile2)

            commandMap.register("pluginloader", CommandPlu(this))
        }
        println("PluginLoader loaded in ${time}ms")
    }

    override fun onDisable() {
        controller.unloadAll()
    }

    private fun saveCfg(){
        config["cache"] = controller.cache
        config["mavenCache"] = controller.mavenCache
        config["repos"] = repos
        config["plugins"] = pluginsList
        config.save(configFile2)
    }

    private fun defaultConfig(){
        if(!configFile2.exists()) Files.createFile(configFile2.toPath())
        config["cache"] = controller.cache
        config["mavenCache"] = controller.mavenCache
        config["repos"] = listOf("plugins/plugins")
        config["plugins"] = listOf("example")
        config.save(configFile2)
    }

    private fun load(jar: File){
        if(!jar.name.endsWith(".jar"))return
        controller.load(jar)
    }

    private fun loadFromRepos(name: String){
        if(pluginsList.contains(name))return
        pluginsList.add(name)
        repos.forEach{
            if(controller.exists(name))return@forEach
            val jar = File(it, "$name.jar")
            if(!jar.exists())return@forEach
            load(jar)
            return
        }
    }

    fun forEach(callback: (String, List<String>) -> Unit) = controller.forEach(callback)

    fun exists(name: String) = controller.exists(name)

    fun load(name: String): Boolean{
        if(exists(name)){
            controller.reload(name)
            return true
        }
        repos.forEach{
            val jar = File(it, "$name.jar")
            if(!jar.exists())return@forEach
            pluginsList.add(name)
            load(jar)
            saveCfg()
            return true
        }
        return false
    }

    fun unload(name: String): Boolean {
        val pre = pluginsList.remove(name)
        if(pre) saveCfg()
        if(!controller.remove(name))return false
        return pre
    }

    fun reload(){
        config.load(configFile2)
        repos.clear()
        repos.addAll(config.getStringList("repos"))
        config.getStringList("plugins").filterNot(pluginsList::contains).forEach(this::loadFromRepos)
        controller.cache = config.string("cache", controller.cache)
        controller.mavenCache = config.string("mavenCache", controller.mavenCache)
    }
}