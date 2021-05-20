package pluginloader.internal.bukkit.shared

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.plugin.EventExecutor
import pluginloader.api.Args
import pluginloader.api.LoaderPlugin
import pluginloader.api.Sender
import pluginloader.api.isNotOp
import pluginloader.internal.shared.unsetFinal
import java.lang.invoke.MethodHandle

class MethodHandleEventExecutor(private val pluginName: String, private val eventClass: Class<*>, private val handler: MethodHandle)
    : EventExecutor {
    override fun execute(listener: org.bukkit.event.Listener, event: Event) {
        if (!eventClass.isInstance(event)) return
        try {
            handler.invokeWithArguments(event)
        } catch (t: Throwable) {
            val stackTrace = t.stackTraceToString()
            val substr = stackTrace.substring(0..stackTrace.indexOf("at ${MethodHandleEventExecutor::class.java.canonicalName}.execute("))
            val str = substr.substring(0..(substr.lastIndexOf("at") - 3))
            System.err.println("Plugin: '$pluginName', event '${eventClass.canonicalName}'")
            System.err.println(str)
        }
    }
}

object EmptyListener: org.bukkit.event.Listener

class CommandWrapper(private val pluginName: String, private val handler: (Sender, Args) -> Unit, private val checkOp: Boolean, name: String, aliases: List<String>):
    org.bukkit.command.Command(name, "", "", aliases) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
        if(checkOp && sender.isNotOp)return true
        try {
            handler.invoke(sender, args)
        } catch (t: Throwable) {
            val stackTrace = t.stackTraceToString()
            System.err.println("Command: '/$commandLabel ${args.joinToString(" ")}'\nPlugin: '$pluginName', user: '${if(sender is Player) sender.name else "CONSOLE"}'")
            System.err.println(stackTrace)
        }
        return true
    }
}

val commandMap: SimpleCommandMap by lazy {
    val field = Bukkit.getServer()::class.java.getDeclaredField("commandMap")
    field.unsetFinal()
    field.get(Bukkit.getServer()) as SimpleCommandMap
}

private val SimpleCommandMap.known: MutableMap<String, Command> by lazy {
    @Suppress("UNCHECKED_CAST")
    commandMap::class.java.getDeclaredField("knownCommands").unsetFinal().get(commandMap) as MutableMap<String, Command>
}

fun registerCommand(plugin: LoaderPlugin, name: String, callback: (Sender, Args) -> Unit, checkOp: Boolean, vararg aliases: String){
    val wrapper = CommandWrapper(name, callback, checkOp, name, aliases.asList())
    commandMap.register("pluginloader", wrapper)
    plugin.unloadHandler {
        val iterator = commandMap.known.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value != wrapper) continue
            iterator.remove()
        }
        wrapper.unregister(commandMap)
    }
}