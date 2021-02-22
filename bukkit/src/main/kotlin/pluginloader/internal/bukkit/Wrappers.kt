package pluginloader.internal.bukkit

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventException
import org.bukkit.plugin.EventExecutor
import pluginloader.api.Args
import pluginloader.api.Sender
import pluginloader.api.isNotOp
import java.lang.invoke.MethodHandle

internal class MethodHandleEventExecutor(private val pluginName: String, private val eventClass: Class<*>, private val handler: MethodHandle)
    : EventExecutor {
    @Throws(EventException::class)
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

internal object EmptyListener: org.bukkit.event.Listener

internal class CommandWrapper(private val pluginName: String, private val handler: (Sender, Args) -> Unit, private val checkOp: Boolean, name: String, aliases: List<String>):
    org.bukkit.command.Command(name, "", "", aliases) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
        if(checkOp && sender.isNotOp)return true
        try {
            handler.invoke(sender, args)
        } catch (t: Throwable) {
            val stackTrace = t.stackTraceToString()
            val substr = stackTrace.substring(0..stackTrace.indexOf("at ${CommandWrapper::class.java.canonicalName}.execute("))
            val str = substr.substring(0..(substr.lastIndexOf("at")))
            val st = str.substring(0..(str.lastIndexOf("at")))
            System.err.println("Command: '/$commandLabel ${args.joinToString(" ")}'\nPlugin: '$pluginName', user: '${if(sender is Player) sender.name else "CONSOLE"}'")
            System.err.println(str.substring(0..(st.lastIndexOf("at") - 3)))
        }
        return true
    }
}