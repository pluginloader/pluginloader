package pluginloader.internal.bukkit

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import pluginloader.api.onlinePlayers
import pluginloader.api.plugin
import pluginloader.internal.bukkit.nms.v1_12_R1.check_1_12_R1
import pluginloader.internal.bukkit.nms.v1_16_R2.check_1_16_R2
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
        when{
            check_1_12_R1() -> {}
            check_1_16_R2() -> {}
        }
        plugin = this
        val time = measureTimeMillis {
            register(controller)
            val pluginsDir = File(file.parentFile, "plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdir()
            if (configFile2.exists()) config.load(configFile2)
            else defaultConfig()
            repos.addAll(config.getStringList("repos"))
            config.getStringList("plugins").forEach(this::loadFromRepos)
            config.save(configFile2)

            Bukkit.getCommandMap().register("pluginloader", CommandPlu(this))
        }
        println("PluginLoader loaded in ${time}ms")
    }

    override fun onDisable() {
        controller.unloadAll()
    }

    private fun saveCfg(){
        config["repos"] = repos
        config["plugins"] = pluginsList
        config.save(configFile2)
    }

    private fun defaultConfig(){
        if(!configFile2.exists()) Files.createFile(configFile2.toPath())
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

    fun toggleRepo(repo: String): Boolean{
        if(repos.remove(repo))return true
        else repos.add(repo)
        saveCfg()
        return false
    }
}