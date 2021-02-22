package pluginloader.api

import com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.plugin.Plugin
import java.util.*

lateinit var plugin: Plugin

typealias Sender = CommandSender
typealias Args = Array<String>

inline fun isBukkit(): Boolean{
    return try{
        Bukkit::class.java.simpleName
        true
    }catch (ex: NoClassDefFoundError){
        false
    }
}

val Sender.isNotOp get() = !this.isOp

val Player.uuid get() = this.uniqueId

val UUID.player get(): Player? = Bukkit.getPlayer(this)

val PlayerEvent.uuid get() = this.player.uuid

fun Cancellable.cancel() { this.isCancelled = true }

fun PlayerInitialSpawnEvent.spawn(location: Location) { this.spawnLocation = location }

val EntityDeathEvent.killer: Player? get() = this.entity.killer

inline val onlinePlayers get() = Bukkit.getOnlinePlayers()

@Serializable
open class V3(open val x: Double, open val y: Double, open val z: Double){
    constructor(x: Int, y: Int, z: Int): this(x.toDouble(), y.toDouble(), z.toDouble())

    val blockX: Int get() = x.toInt()
    val blockY: Int get() = y.toInt()
    val blockZ: Int get() = z.toInt()

    open val center: V3 get() = V3(blockX + 0.5, blockY.toDouble(), blockZ + 0.5)
    open val block: V3 get() = V3(blockX.toDouble(), blockY.toDouble(), blockZ.toDouble())

    open fun location(world: World): Location = Location(world, x, y, z)
    fun location(world: String): Location? {
        return location(Bukkit.getWorld(world) ?: return null)
    }

    fun block(world: World): Block = world.getBlockAt(blockX, blockY, blockZ)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null)return false
        if (!javaClass.isInstance(other)) return false

        other as V3

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    override fun toString() = "V3(x=$x, y=$y, z=$z)"

    open fun toConfig(config: Configuration) =
            config.setDouble("x", x).setDouble("y", y).setDouble("z", z)

    companion object{
        fun parse(section: ConfigurationSection) =
                V3(section.double("x"), section.double("y"), section.double("z"))
        val empty = V3(0, 0, 0)
    }
}

@Serializable
open class V5: V3{
    val yaw: Float
    val pitch: Float

    constructor(x: Double, y: Double, z: Double, yaw: Float = 0F, pitch: Float = 0F): super(x, y, z){
        this.yaw = yaw
        this.pitch = pitch
    }

    constructor(x: Int, y: Int, z: Int, yaw: Float = 0F, pitch: Float = 0F): this(x.toDouble(), y.toDouble(), z.toDouble(), yaw, pitch)

    override val center: V5 get() = V5(blockX + 0.5, blockY.toDouble(), blockZ + 0.5, yaw, pitch)
    override val block: V5 get() = V5(blockX.toDouble(), blockY.toDouble(), blockZ.toDouble(), 0F, 0F)

    override fun location(world: World): Location = Location(world, x, y, z, yaw, pitch)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other == null)return false
        if (!javaClass.isInstance(other)) return false

        other as V5

        if (yaw != other.yaw) return false
        if (pitch != other.pitch) return false

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + pitch.hashCode()
        return result
    }

    override fun toString() = "V5(x=$x, y=$y, z=$z, yaw=$yaw, pitch=$pitch)"

    override fun toConfig(config: Configuration) =
        super.toConfig(config).setFloat("yaw", yaw).setFloat("pitch", pitch)

    companion object{
        fun parse(section: ConfigurationSection) = V5(
                section.double("x"), section.double("y"), section.double("z"),
                section.float("yaw"), section.float("pitch"))
        val empty = V5(0, 0, 0)
    }
}

@Serializable
open class Loc: V5{
    val world: String

    constructor(x: Double, y: Double, z: Double, yaw: Float = 0F, pitch: Float = 0F, world: String = "world"): super(x, y, z, yaw, pitch){
        this.world = world
    }

    constructor(x: Int, y: Int, z: Int, yaw: Float = 0F, pitch: Float = 0F, world: String = "world"): this(x.toDouble(), y.toDouble(), z.toDouble(), yaw, pitch, world)

    fun location(): Location? = location(world)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Loc

        if (world != other.world) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + world.hashCode()
        return result
    }

    override fun toString(): String {
        return "Loc(x=$x, y=$y, z=$z, yaw=$yaw, pitch=$pitch, world=$world)"
    }

    companion object{
        val empty = Loc(0, 0, 0)
    }
}