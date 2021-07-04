package pluginloader.internal.bukkit.plugin

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

internal class CommandPlu(private val plugin: JavaPlugin): Command("pluginloader", "", "", listOf("plu")){
    private val prefix = "§8[§aPlu§8]§f"

    override fun execute(sender: CommandSender, commandLabel: String?, args: Array<String>): Boolean {
        if(!sender.isOp)return true
        if(args.isEmpty()){
            sender.sendMessage("$prefix Usage: §6/pluginloader [load, unload, reload, reloadCfg, show, install, uninstall, installed, version]")
            return true
        }
        val name = if(args.size == 1) "" else args[1]
        when (args[0].lowercase()) {
            "load", "l" -> {
                if(args.size == 1){
                    sender.sendMessage("$prefix Usage: §6/pluginloader load [plugin]")
                    return true
                }
                val exists = plugin.exists(name)
                if(plugin.load(name)){
                    sender.sendMessage(if(exists) "$prefix §6'$name'§f reloaded" else "$prefix §6'$name'§f loaded")
                }else{
                    sender.sendMessage("$prefix §6'$name'§c not found")
                }
            }
            "unload", "u" -> {
                if(args.size == 1){
                    sender.sendMessage("$prefix Usage: §6/pluginloader unload [plugin]")
                    return true
                }
                if(plugin.unload(name)){
                    sender.sendMessage("$prefix §6'$name'§f unloaded")
                }else{
                    sender.sendMessage("$prefix §6'$name'§c not found")
                }
            }
            "reloadcfg", "r" -> {
                plugin.reload()
                sender.sendMessage("$prefix Reloaded config.yml")
            }
            "reload" -> {
                if(args.size == 1){
                    sender.sendMessage("$prefix Usage: §6/pluginloader reload [plugin]")
                    return true
                }
                if(plugin.exists(name)){
                    plugin.load(name)
                    sender.sendMessage("$prefix §6'$name'§f reloaded")
                }else{
                    sender.sendMessage("$prefix §6'$name'§c not found")
                }
            }
            "show", "s" -> {
                if(args.size >= 2){
                    val filterByDependency = args.size == 3 && args[2].contains("d")
                    sender.sendMessage("$prefix Filtered by ${if(filterByDependency) "dependency" else "name"} plugins")
                    plugin.forEach { pluginName, dependency ->
                        if(filterByDependency){
                            var contains = false
                            dependency.forEach{if(it.contains(args[1], ignoreCase = true))contains = true}
                            if(!contains)return@forEach
                        } else {
                            if (pluginName.contains(args[1], ignoreCase = true).not()) return@forEach
                        }
                        sender.sendMessage("$prefix §6'$pluginName'${if(dependency.isNotEmpty()) "§f, dependency: §6${dependency.joinToString(separator = "§f, §6")}" else ""}")
                    }
                }else {
                    sender.sendMessage("$prefix All plugins")
                    plugin.forEach { pluginName, dependency ->
                        sender.sendMessage("$prefix §6'$pluginName'${if(dependency.isNotEmpty()) "§f, dependency: §6'${dependency.joinToString(separator = "'§f, §6'")}" else ""}")
                    }
                }
            }
            "i", "install" -> {
                if(!plugin.repoSupport()){
                    sender.sendMessage("$prefix Repo support is disabled")
                    return true
                }
                if(args.size == 1){
                    sender.sendMessage("$prefix Usage: §6/pluginloader install [plugin]")
                    return true
                }
                val split = name.split(":")
                val group = if(split.size == 1) "pluginloader" else split[0]
                val pluginName = if(split.size == 1) split[0] else split[1]
                if(plugin.exists(pluginName)){
                    sender.sendMessage("$prefix Unload §6'$pluginName'§f before installing")
                    return true
                }
                if(plugin.downloadPlugin("$group:$pluginName")){
                    plugin.saveCfg()
                    sender.sendMessage("$prefix Installed §6'$name'")
                }else{
                    sender.sendMessage("$prefix §6'$name'§f not found in repos")
                }
            }
            "uninstall" -> {
                if(args.size == 1){
                    sender.sendMessage("$prefix Usage: §6/pluginloader uninstall [plugin]")
                    return true
                }
                val split = name.split(":")
                val group = if(split.size == 1) "pluginloader" else split[0]
                val pluginName = if(split.size == 1) split[0] else split[1]
                if(plugin.uninstallPlugin("$group:$pluginName")){
                    sender.sendMessage("$prefix §6'$name'§c not found")
                }else{
                    sender.sendMessage("$prefix §6'$name'§f uninstalled")
                }
            }
            "installed" -> {
                sender.sendMessage("$prefix All installed plugins")
                plugin.installed().forEach{sender.sendMessage("$prefix §6'$it'")}
            }
            "version" -> {
                sender.sendMessage("$prefix Current version: §6'1.9.0'")
            }
            else -> {
                sender.sendMessage("$prefix Unknown subcommand")
            }
        }
        return true
    }
}