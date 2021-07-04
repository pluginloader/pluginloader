package pluginloader.api

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

fun runTask(task: () -> Unit): BukkitTask = scheduler().runTask(plugin, Task(task))

fun runTaskLater(time: Int, task: () -> Unit): BukkitTask = scheduler().runTaskLater(plugin, Task(task), time.toLong())

fun runTaskTimer(time: Int, task: () -> Unit): BukkitTask = scheduler().runTaskTimer(plugin, Task(task), time.toLong(), time.toLong())

fun runAsync(task: () -> Unit): BukkitTask = scheduler().runTaskAsynchronously(plugin, Task(task))

fun runAsyncLater(time: Int, task: () -> Unit): BukkitTask = scheduler().runTaskLaterAsynchronously(plugin, Task(task), time.toLong())

fun runAsyncTimer(time: Int, task: () -> Unit): BukkitTask = scheduler().runTaskTimerAsynchronously(plugin, Task(task), time.toLong(), time.toLong())

private fun scheduler() = Bukkit.getScheduler()

private class Task(private val task: () -> Unit): Runnable{
    override fun run() {
        task()
    }
}