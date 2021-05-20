package pluginloader.internal.bukkit.plugin

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import pluginloader.internal.bukkit.shared.BukkitPlugin
import pluginloader.internal.shared.InternalPlugin
import pluginloader.internal.shared.PluginController
import java.io.File
import java.nio.file.Files

internal class Plugin(controller: PluginController, name: String, jar: File): InternalPlugin(controller, name), pluginloader.api.Plugin, BukkitPlugin {
    private val configFile = File(jar.parent, jar.name.replace(".jar", ".yml"))
    private val configPath = configFile.toPath()
    private val _config by lazy(LazyThreadSafetyMode.NONE) {
        val conf = YamlConfiguration()
        if (Files.exists(configPath)) conf.load(configFile)
        conf
    }
    override val config: FileConfiguration get() = _config
    private var reloaded = false
    override var configSave = false

    override fun saveConfig() {
        if (Files.notExists(configPath)) Files.createFile(configFile.toPath())
        _config.save(configFile)
    }

    override fun unload(){
        reloaded = true
        super.unload()
    }

    override fun startLoad(){
        if(reloaded && Files.exists(configPath)) _config.load(configFile)
        if(configSave){
            _config.save(configFile)
            configSave = false
        }
    }
}