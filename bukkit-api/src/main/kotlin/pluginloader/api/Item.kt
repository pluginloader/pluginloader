package pluginloader.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SpawnEggMeta
import pluginloader.api.bukkit.NBT
import java.util.*

@Serializable
class Item: Cloneable{
    @Transient
    val meta = ArrayList<(ItemMeta) -> Unit>(3)
    var type: Material = Material.STONE
    var amount: Int = 1
    var damage: Int = 0
    var name: String = ""
    val lore: ArrayList<String> = ArrayList(3)
    val enchantment: MutableMap<@Serializable(EnchantmentSerializer::class) Enchantment, Int> = HashMap(3)
    @SerialName("data")
    var nbt: NBT = NBT.new()
    var unbreakable: Boolean = false
    var flags: MutableSet<ItemFlag> = EnumSet.noneOf(ItemFlag::class.java)

    fun item(): ItemStack{
        val item = ItemStack(type, amount, damage.toShort())
        if(type == Material.AIR)return item
        val meta = item.itemMeta
        if(name.isNotEmpty())meta.displayName = name
        if(lore.isNotEmpty())meta.lore = lore
        enchantment.forEach{meta.addEnchant(it.key, it.value, true)}
        meta.isUnbreakable = unbreakable
        flags.forEach(meta::addItemFlags)
        this.meta.forEach{it(meta)}
        item.itemMeta = meta
        this.nbt.writeTo(item)
        return item
    }

    inline fun replaceLore(noinline replace: (String) -> String){
        lore.replaceAll(replace)
    }

    companion object{
        fun item(stack: ItemStack): Item{
            val item = Item()
            item.type = stack.type
            item.amount = stack.amount
            item.damage = stack.durability.toInt()
            if(stack.hasItemMeta()){
                val meta = stack.itemMeta
                if(meta.hasDisplayName())item.name = meta.displayName
                if(meta.hasLore())item.lore.addAll(meta.lore)
                if(meta.hasEnchants())item.enchantment.putAll(meta.enchants)
                item.flags.addAll(meta.itemFlags)
                item.unbreakable = meta.isUnbreakable
                item.nbt = NBT.clone(stack)
            }
            return item
        }

        fun default() = Item().type(Material.STONE)

        @Deprecated("don't use :/")
        fun parse(section: ConfigurationSection, log: (String) -> Unit = System.out::println): Item{
            val item = default()
            val currentPath = section.currentPath

            val type = section.getString("item")
            if(type != null) {
                item.type = try {
                    Material.valueOf(type.uppercase())
                } catch (ex: java.lang.IllegalArgumentException) {
                    log("Сложна-а-а, я хз что такое '$type', нет такого предмета в словарике, чекни чо ты написал в '$currentPath'")
                    Material.CAKE
                }
            }

            item.amount = section.getInt("amount", 1)

            val textStr = section.readableString("text")
            if(textStr != null){
                val words = textStr.split("\n")
                if(words.isNotEmpty()){
                    item.name = words[0]
                    for(i in 1 until words.size)
                        item.lore.add(words[i])
                }
            }

            val mobType = section.getString("mob")
            if(mobType != null) {
                val parse = try {
                    EntityType.valueOf(type.uppercase())
                } catch (ex: java.lang.IllegalArgumentException) {
                    println("кто такой '$mobType' в предмете '$currentPath', я ево не знаю, он летучая мыш?")
                    EntityType.BAT
                }
                item.meta<SpawnEggMeta>{ it.spawnedType = parse}
            }

            val color = section.getString("color")
            if(color != null){
                try {
                    item.color(Color.valueOf(color.uppercase()))
                } catch (ex: IllegalArgumentException) {
                    log("В душе ниибу шо за цвет $color")
                }
            }

            if (section.getBoolean("unbreakable")) item.unbreakable()

            val itemFlags = section.getStringList("flags")
            if (itemFlags != null && item.type != Material.END_CRYSTAL) {
                itemFlags.forEach{flag ->
                    try {
                        if (flag == "*") item.flags(*ItemFlag.values())
                        else item.flag(ItemFlag.valueOf(flag.uppercase()))
                    } catch (ex: IllegalArgumentException) {
                        log("Флаг $flag это какая-то фигня, убери его из '$currentPath'")
                    }
                }
            }

            val nbt = section.getConfigurationSection("nbt")
            if (nbt != null) {
                for (key in nbt.getKeys(false)) {
                    when (val value = nbt[key]) {
                        is Int -> item[key] = value.toInt()
                        is Double -> item[key] = value.toDouble()
                        is String -> item[key] = value.toString()
                        else -> log("Непонятное значение тега: '$value', path: $currentPath.nbt")
                    }
                }
            }
            return item
        }

        fun parseStack(section: ConfigurationSection, log: (String) -> Unit = System.out::println): ItemStack{
            return parse(section, log).item()
        }
    }

    //kotlin...
    fun amount(amount: Int): Item{
        this.amount = amount
        return this
    }

    fun damage(damage: Int): Item{
        this.damage = damage
        return this
    }

    fun color(color: Color): Item {
        return damage(color.woolData)
    }

    fun type(type: Material): Item{
        this.type = type
        return this
    }

    fun name(name: String): Item{
        this.name = name
        return this
    }

    fun lore(vararg lores: String): Item{
        lore.addAll(lores)
        return this
    }

    fun replaceLore(vararg lores: String): Item{
        lore.clear()
        lore.addAll(lores)
        return this
    }

    fun enchantment(enchantment: Enchantment, level: Int): Item{
        this.enchantment[enchantment] = level
        return this
    }

    fun unbreakable(): Item{
        unbreakable = true
        return this
    }

    fun flag(flag: ItemFlag): Item{
        flags.add(flag)
        return this
    }

    fun flags(vararg flag: ItemFlag): Item {
        flags.addAll(flag)
        return this
    }

    fun <T : ItemMeta> meta(meta: (T) -> Unit): Item{
        @Suppress("UNCHECKED_CAST")
        this.meta.add{meta(it as T)}
        return this
    }

    fun hasNbt(key: String): Boolean = this.nbt.has(key)
    fun stringNbt(key: String): String = this.nbt.string(key, "") ?: ""
    fun intNbt(key: String): Int = this.nbt.int(key, 0)
    fun doubleNbt(key: String): Double = this.nbt.double(key, 0.0)
    operator fun set(key: String, value: String) = this.nbt.setString(key, value)
    operator fun set(key: String, value: Int) = this.nbt.setInt(key, value)
    operator fun set(key: String, value: Double) = this.nbt.setDouble(key, value)

    public override fun clone(): Item {
        val item = Item()
        item.meta.addAll(meta)
        item.amount = amount
        item.damage = damage
        item.type = type
        item.name = name
        item.lore.addAll(lore)
        item.enchantment.putAll(enchantment)
        item.nbt = this.nbt.clone()
        item.unbreakable = unbreakable
        item.flags.addAll(flags)
        return item
    }

    override fun toString(): String {
        return "Item(meta=$meta, type=$type, amount=$amount, damage=$damage, name='$name', lore=$lore, enchantment=$enchantment, nbt=$nbt, unbreakable=$unbreakable, flags=$flags)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        if (meta != other.meta) return false
        if (type != other.type) return false
        if (amount != other.amount) return false
        if (damage != other.damage) return false
        if (name != other.name) return false
        if (lore != other.lore) return false
        if (enchantment != other.enchantment) return false
        if (this.nbt != other.nbt) return false
        if (unbreakable != other.unbreakable) return false
        if (flags != other.flags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = meta.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + amount
        result = 31 * result + damage
        result = 31 * result + name.hashCode()
        result = 31 * result + lore.hashCode()
        result = 31 * result + enchantment.hashCode()
        result = 31 * result + nbt.hashCode()
        result = 31 * result + unbreakable.hashCode()
        result = 31 * result + flags.hashCode()
        return result
    }
}