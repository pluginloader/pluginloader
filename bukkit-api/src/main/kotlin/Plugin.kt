package pluginloader.api

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.scheduler.BukkitTask

interface Plugin: LoaderPlugin {
    @Deprecated("Use plugin configs:1.0.0")
    val config: ConfigurationSection

    @Deprecated("Use plugin configs:1.0.0")
    fun saveConfig()

    fun registerCommand(name: String, callback: (Sender, Args) -> Unit, checkOp: Boolean = false, vararg aliases: String)

    override fun unloadHandler(handler: () -> Unit)

    //Binary comparable
    @Deprecated("use other functions lol", level = DeprecationLevel.HIDDEN)
    fun runTaskTimer(time: Int, callback: () -> Unit){
        runTaskTimer(time, callback, false)
    }
}

fun LoaderPlugin.runTaskLater(time: Int, callback: () -> Unit, callOnUnload: Boolean = false){
    runAbstractTask({Bukkit.getScheduler().runTaskLater(plugin, it, time.toLong())}, callback, callOnUnload, true)
}

fun LoaderPlugin.runAsyncLater(time: Int, callback: () -> Unit, callOnUnload: Boolean = false){
    runAbstractTask({Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, it, time.toLong())}, callback, callOnUnload, true)
}

fun LoaderPlugin.runTaskTimer(time: Int, callback: () -> Unit, callOnUnload: Boolean = false){
    runAbstractTask({Bukkit.getScheduler().runTaskTimer(plugin, it, time.toLong(), time.toLong())}, callback, callOnUnload, false)
}

fun LoaderPlugin.runAsyncTimer(time: Int, callback: () -> Unit, callOnUnload: Boolean = false){
    runAbstractTask({Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, it, time.toLong(), time.toLong())}, callback, callOnUnload, false)
}

private fun LoaderPlugin.runAbstractTask(task: (() -> Unit) -> BukkitTask, callback: () -> Unit, callOnUnload: Boolean, singleCall: Boolean){
    var unload: (() -> Unit)? = null
    val bukkitTask = if(singleCall) task {
        unload.nonNull(this::removeUnloadHandler)
        callback()
    } else task(callback)
    unload = {
        if(!bukkitTask.isCancelled) {
            bukkitTask.cancel()
            if (callOnUnload) callback()
        }
    }
    unloadHandler(unload)
}