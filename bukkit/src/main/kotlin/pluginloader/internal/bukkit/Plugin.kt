package pluginloader.internal.bukkit

import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pluginloader.api.Args
import pluginloader.api.Sender
import pluginloader.api.runTask
import pluginloader.internal.shared.InternalPlugin
import pluginloader.internal.shared.PluginController
import java.io.File
import java.nio.file.Files

internal class Plugin(controller: PluginController, name: String, val jar: File): InternalPlugin(controller, name), pluginloader.api.Plugin {
    private val configFile = File(jar.parent, jar.name.replace(".jar", ".yml"))
    private val configPath = configFile.toPath()
    internal val _config by lazy(LazyThreadSafetyMode.NONE) {
        val conf = YamlConfiguration()
        if (Files.exists(configPath)) conf.load(configFile)
        conf
    }
    override val config: FileConfiguration get() = _config
    private var reloaded = false
    var configSave = false

    override fun saveConfig() {
        if (Files.notExists(configPath)) Files.createFile(configFile.toPath())
        _config.save(configFile)
    }

    override fun registerCommand(name: String, callback: (Sender, Args) -> Unit, checkOp: Boolean, vararg aliases: String) {
        val wrapper = CommandWrapper(name, callback, checkOp, name, aliases.asList())
        Bukkit.getCommandMap().register("pluginloader", wrapper)
        this.unloadHandler {
            val iterator = Bukkit.getCommandMap().knownCommands.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value != wrapper) continue
                iterator.remove()
            }
            wrapper.unregister(Bukkit.getCommandMap())
        }
    }

    override fun task(task: () -> Unit) {
        runTask(task)
    }

    override fun unload(){
        reloaded = true
        super.unload()
    }

    override fun startLoad(){
        if(reloaded && Files.exists(configPath)) _config.load(configFile)
        if(configSave){
            config.save(configFile)
            configSave = false
        }
    }
}