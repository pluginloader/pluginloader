package pluginloader.internal.bukkit.single

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import pluginloader.api.plugin
import pluginloader.internal.bukkit.shared.BukkitPlugin
import pluginloader.internal.bukkit.shared.register
import pluginloader.internal.shared.InternalPlugin
import pluginloader.internal.shared.PluginController

class JavaPlugin: org.bukkit.plugin.java.JavaPlugin() {
    private val controller = PluginController({error("plugins can't have dependency")}, { _, _, _ -> error("plugin can't be loaded")})

    override fun onEnable() {
        plugin = this
        register(controller)
        val loader = JavaPlugin::class.java.classLoader
        val plugins = loader.getResourceAsStream("plugins.txt")!!.readBytes().decodeToString().split("\n")
        plugins.forEach{
            val split = it.split("=")
            val plugin = Plugin(loader, controller, split[0])
            plugin.mayExceptionLoad(loader.loadClass(split[1]).kotlin)?.printStackTrace()
        }
    }

    override fun onDisable() {
        controller.unloadAll()
    }
}

private class Plugin(loader: ClassLoader, controller: PluginController, name: String): InternalPlugin(controller, name), BukkitPlugin{
    override var configSave: Boolean
        get() = false
        set(_) {}

    private val _config: ConfigurationSection
    override val config: ConfigurationSection get() = _config

    init {
        _config = YamlConfiguration()
        val conf = loader.getResourceAsStream("$name.yml")
        if(conf != null) _config.loadFromString(conf.readBytes().decodeToString())
    }

    override fun saveConfig() {
        //configs save not supported
    }
}