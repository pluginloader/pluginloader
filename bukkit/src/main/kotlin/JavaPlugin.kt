package pluginloader.internal.bukkit.plugin

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import pluginloader.api.*
import pluginloader.internal.shared.PluginController
import pluginloader.internal.shared.PluginRepo
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

class JavaPlugin {
    private lateinit var file: File
    private val configFile2 by lazy{File(file.parentFile, "plugins/config.yml")}
    private val config = YamlConfiguration()
    private val repos = ArrayList<String>()
    private val pluginsList = ArrayList<String>()
    private val downloadedPlugins = ArrayList<String>()
    private var repoSupport = true
    private val controller = PluginController(this::load, ::Plugin){plu, ex ->
        val message = "Plugin ${plu.name} exception:\n${ex.stackTraceToString()}"
        onlinePlayers.forEach{if(it.isOp)it.sendMessage(message)}
        System.err.println("Plugin ${plu.name}, exception:")
        ex.printStackTrace()
    }

    fun load(bukkitPlugin: JavaPlugin, file: File) {
        this.file = file
        plugin = bukkitPlugin
        val time = measureTimeMillis {
            register(controller)
            val pluginsDir = File(file.parentFile, "plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdir()
            if (configFile2.exists()) config.load(configFile2)
            else defaultConfig()

            controller.cache = config.string("cache", controller.cache)
            controller.mavenCache = config.string("mavenCache", controller.mavenCache)
            repos.addAll(config.getStringList("repos"))
            config.getStringList("downloadedPlugins").forEach(this::downloadPlugin)
            config.getStringList("plugins").forEach(this::loadFromRepos)
            config["repoSupport"] = config.getBoolean("repoSupport", true)

            val oldFile = File(controller.cache, "old.txt")
            if(oldFile.exists()){
                runTaskLater(1){
                    File(file.parentFile, oldFile.readText()).delete()
                    oldFile.delete()
                }
            }
            repoSupport = config.getBoolean("repoSupport", true)

            config.save(configFile2)

            commandMap.register("pluginloader", CommandPlu(this))
        }
        println("PluginLoader loaded in ${time}ms")
    }

    fun unload() {
        controller.unloadAll()
    }

    fun installed(): List<String>{
        return downloadedPlugins
    }

    fun repoSupport(): Boolean{
        return repoSupport
    }

    fun saveCfg(){
        config["cache"] = controller.cache
        config["mavenCache"] = controller.mavenCache
        config["repos"] = repos
        config["plugins"] = pluginsList
        config["downloadedPlugins"] = downloadedPlugins
        config.save(configFile2)
    }

    private fun defaultConfig(){
        if(!configFile2.exists()) Files.createFile(configFile2.toPath())
        config["cache"] = controller.cache
        config["mavenCache"] = controller.mavenCache
        config["repos"] = listOf("plugins/plugins")
        config["plugins"] = listOf("example")
        config["downloadedPlugins"] = listOf<String>()
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

    fun downloadPlugin(name: String): Boolean{
        if(downloadedPlugins.contains(name))return true
        val split = name.split(":")
        PluginRepo.official.get(File(controller.cache, "repo"), split[0], split[1]).nonNull {
            controller.load(it)
            downloadedPlugins.add(name)
            return true
        }
        return false
    }

    fun uninstallPlugin(name: String): Boolean{
        if(!downloadedPlugins.remove(name))return false
        controller.remove(name.split(":")[1])
        return true
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
        repoSupport = config.boolean("repoSupport", true)
    }
}