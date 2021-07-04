package pluginloader.api

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

typealias Configuration = ConfigurationSection

fun Configuration.readableString(path: String): String?{
    return (this.getString(path) ?: return null).replace("&", "§")
}

fun config(): Configuration = MemoryConfiguration()

fun Configuration.getFloat(key: String, value: Float = 0.0F) = getDouble(key, value.toDouble()).toFloat()

fun Configuration.string(key: String, default: String = "") = getString(key, default)
fun Configuration.int(key: String, default: Int = 0) = getInt(key, default)
fun Configuration.boolean(key: String, default: Boolean = false) = getBoolean(key, default)
fun Configuration.double(key: String, default: Double = 0.0) = getDouble(key, default)
fun Configuration.float(key: String, default: Float = 0.0F) = getFloat(key, default)
fun Configuration.long(key: String, default: Long = 0L) = getLong(key, default)
fun Configuration.section(key: String): Configuration? = getConfigurationSection(key)

fun Configuration.setString(key: String, value: String): Configuration{
    set(key, value)
    return this
}

fun Configuration.setInt(key: String, value: Int): Configuration{
    set(key, value)
    return this
}

fun Configuration.setBoolean(key: String, value: Boolean): Configuration{
    set(key, value)
    return this
}

fun Configuration.setDouble(key: String, value: Double): Configuration{
    set(key, value)
    return this
}

fun Configuration.setFloat(key: String, value: Float): Configuration{
    set(key, value)
    return this
}

fun Configuration.setLong(key: String, value: Long): Configuration{
    set(key, value)
    return this
}

fun Configuration.setSection(key: String, value: Configuration): Configuration{
    set(key, value)
    return this
}