package pluginloader.internal.bukkit.plugin

import org.bukkit.plugin.java.JavaPlugin

class Loader: JavaPlugin() {
    private val plu = pluginloader.internal.bukkit.plugin.JavaPlugin()

    override fun onEnable() {
        plu.load(this, file)
    }

    override fun onDisable() {
        plu.unload()
    }
}