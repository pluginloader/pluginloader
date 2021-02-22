package pluginloader.internal.bukkit

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

internal class CommandPlu(private val plugin: JavaPlugin): Command("pluginloader", "", "", listOf("plu")){
    override fun execute(sender: CommandSender, commandLabel: String?, args: Array<String>): Boolean {
        @Suppress("UNCHECKED_CAST")
        if(!sender.isOp)return true
        if(args.isEmpty()){
            sender.sendMessage("§6Usage: /pluginloader [Load|Unload|Toggle|Show]")
            return true
        }
        val name = if(args.size == 1) "" else args[1]
        when (args[0].toLowerCase()) {
            "load", "l" -> {
                val exists = plugin.exists(name)
                if(plugin.load(name)){
                    sender.sendMessage(if(exists) "§6Ok, $name reloaded" else "§6Ok, $name loaded")
                }else{
                    sender.sendMessage("§cNot ok, $name not found")
                }
            }
            "unload", "u" -> {
                if(plugin.unload(name)){
                    sender.sendMessage("§6Ok, $name unloaded")
                }else{
                    sender.sendMessage("§cNot ok, $name not found")
                }
            }
            "toggle", "t" -> {
                if(plugin.toggleRepo(name)){
                    sender.sendMessage("§6Repo $name removed")
                }else{
                    sender.sendMessage("§6Repo $name added")
                }
            }
            "show", "s" -> {
                if(args.size >= 2){
                    val filterByDependency = args.size == 3 && args[2].contains("d")
                    sender.sendMessage("§6Filtered by ${if(filterByDependency) "dependency" else "name"} plugins")
                    plugin.forEach { pluginName, dependency ->
                        if(filterByDependency){
                            var contains = false
                            dependency.forEach{if(it.contains(args[1], ignoreCase = true))contains = true}
                            if(!contains)return@forEach
                        } else {
                            if (pluginName.contains(args[1], ignoreCase = true).not()) return@forEach
                        }
                        sender.sendMessage("§6Plugin $pluginName")
                        if (dependency.isNotEmpty()) sender.sendMessage("§6Dependency: ${dependency.joinToString(separator = ", ")}")
                    }
                }else {
                    sender.sendMessage("§6All plugins")
                    plugin.forEach { pluginName, dependency ->
                        sender.sendMessage("§6Plugin $pluginName")
                        if (dependency.isNotEmpty()) sender.sendMessage("§6Dependency: ${dependency.joinToString(separator = ", ")}")
                    }
                }
            }
            else -> {
                sender.sendMessage("§Unknown subcommand")
            }
        }
        return true
    }
}