package pluginloader.internal.bukkit.plugin

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.plugin.RegisteredListener
import pluginloader.api.*
import pluginloader.internal.shared.PluginController
import pluginloader.internal.shared.unsetFinal
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

interface BukkitPlugin: pluginloader.api.Plugin{
    override fun cmd(command: String, cmd: (CmdSender, Args) -> Unit, vararg aliases: String) {
        registerCommand(command, {sender, args ->
            cmd(object: CmdSender{
                override fun sendMessage(string: String) {
                    sender.sendMessage(string)
                }
            }, args)
        }, true, *aliases)
    }

    override fun registerCommand(name: String, callback: (Sender, Args) -> Unit, checkOp: Boolean, vararg aliases: String) {
        registerCommand(this, name, callback, checkOp, *aliases)
    }

    override fun task(task: () -> Unit) {
        runTask(task)
    }

    var configSave: Boolean
}

fun register(controller: PluginController){
    register({ it.toConfig(config()) }, V3::parse)
    register({ it.toConfig(config()) }, V5::parse)
    register({
        config()
            .setString("host", "localhost")
            .setInt("port", 3306)
            .setString("database", "database")
            .setString("user", "user")
            .setString("password", "password")
    }, Connector::parse)
    fun defaultItem(): Configuration =
        config().setString("item", "STONE").setInt("amount", 1).setInt("damage", 0)
    register({ defaultItem() }, Item::parse)
    register({ defaultItem() }, Item::parseStack)
    register({ it }, { it })

    controller.fieldHandler(Config::class){ field, _, privatePlugin ->
        privatePlugin as BukkitPlugin
        val config = privatePlugin.config
        field.unsetFinal()
        val name = field.name
        val type = field.type
        val obj = config[name]
        if (obj == null) {
            config[name] = when (type) {
                Int::class.java -> field.getInt(null)
                Long::class.java -> field.getLong(null)
                Float::class.java -> field.getFloat(null)
                Double::class.java -> field.getDouble(null)
                String::class.java -> field.get(null)
                else -> (encode[type] ?: return@fieldHandler).invoke(field.get(null))
            }
            privatePlugin.configSave = true
            return@fieldHandler
        }
        when (type) {
            Int::class.java -> field.setInt(null, obj as Int)
            Long::class.java -> field.setLong(null, obj as Long)
            Float::class.java -> field.setFloat(null, obj as Float)
            Double::class.java -> field.setDouble(null, obj as Double)
            String::class.java -> field.set(null, obj)
            else -> field.set(null, (decode[type] ?: return@fieldHandler).invoke(obj as ConfigurationSection))
        }
    }
    controller.methodHandler(Command::class){ method, annotation, pl ->
        pl as BukkitPlugin
        val reflect = method.handle
        val parameterCount = method.parameterCount
        if(parameterCount !in 1..2) error("${method.name} in ${pl.name} contains $parameterCount parameters, need 1 or 2")
        val first = method.parameterTypes[0]
        val handler: (Sender, Args) -> Unit = when{
            parameterCount == 2 && first == Player::class.java -> {sender, args ->
                if(sender !is Player){
                    sender.sendMessage("This command only for players")
                }else{
                    reflect.invokeWithArguments(sender, args)
                }
            }
            parameterCount == 1 && first == Player::class.java -> {sender, _ ->
                if(sender !is Player){
                    sender.sendMessage("This command only for players")
                }else{
                    reflect.invokeWithArguments(sender)
                }
            }
            parameterCount == 2 -> {sender, args -> reflect.invokeWithArguments(sender, args)}
            parameterCount == 1 -> {sender, _ -> reflect.invokeWithArguments(sender)}
            else -> throw Error("kotlin is bad :c")
        }
        pl.registerCommand(annotation.name, handler, annotation.op, *annotation.aliases)
    }
    fun listen(method: Method, ignoreCancelled: Boolean, priority: EventPriority, pl: Plugin) {
        val type = method.parameterTypes[0]
        var clazz = type
        var handlersMethod: Method? = null
        while (handlersMethod == null) {
            if (clazz == java.lang.Object::class.java) {
                println("$type method getHandlerList not exists")
                return
            }
            try {
                handlersMethod = clazz.getDeclaredMethod("getHandlerList")
            }catch(ignored: NoSuchMethodException){}
            clazz = clazz.superclass
        }
        val handlers = handlersMethod.invoke(null) as HandlerList
        val listener = RegisteredListener(
            EmptyListener,
            MethodHandleEventExecutor(pl.name, type, method.handle),
                priority, plugin, ignoreCancelled)
        handlers.register(listener)
        pl.unloadHandler { handlers.unregister(listener) }
    }
    controller.methodHandler(Listener::class){method, listener, plugin -> listen(method, listener.ignoreCancelled, listener.priority, plugin as Plugin)}
    controller.methodHandler(EventHandler::class){ method, listener, plugin -> listen(method, listener.ignoreCancelled, listener.priority, plugin as Plugin)}
}

private val Method.handle: MethodHandle get() = MethodHandles.lookup().unreflect(this)

private inline fun <reified T: Any> register(
        noinline en: (T) -> Any,
        noinline de: (ConfigurationSection) -> T)
    = register(T::class.java, en, de)

private fun <T: Any> register(type: Class<T>, en: (T) -> Any, de: (ConfigurationSection) -> T){
    @Suppress("UNCHECKED_CAST")
    encode[type] = {en.invoke(it as T)}
    decode[type] = de
}

private val encode = HashMap<Class<*>, (Any) -> Any>()
private val decode = HashMap<Class<*>, (ConfigurationSection) -> Any>()
