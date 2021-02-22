package pluginloader.internal.bukkit.nms.v1_12_R1

import kotlinx.serialization.json.*
import net.minecraft.server.v1_12_R1.*
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaBanner
import org.bukkit.inventory.ItemStack
import pluginloader.api.bukkit.NBT
import pluginloader.api.nonNull
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

internal object NBT: NBT.AbstractNBT {
    private val itemMetaGetter: MethodHandle
    private val itemMetaSetter: MethodHandle
    private val itemHandleGetter: MethodHandle
    private val unhandledTagsGetter: MethodHandle
    private val nbtNumberClass: Class<*>
    private val numberE: MethodHandle
    private val numberAsDouble: MethodHandle

    init {
        val lookup = MethodHandles.lookup()

        val itemMeta = ItemStack::class.java.getDeclaredField("meta")
        itemMeta.isAccessible = true
        itemMetaGetter = lookup.unreflectGetter(itemMeta)
        itemMetaSetter = lookup.unreflectSetter(itemMeta)

        val handle = CraftItemStack::class.java.getDeclaredField("handle")
        handle.isAccessible = true
        itemHandleGetter = lookup.unreflectGetter(handle)

        val tags = CraftMetaBanner::class.java.superclass.getDeclaredField("unhandledTags")
        tags.isAccessible = true
        unhandledTagsGetter = lookup.unreflectGetter(tags)

        nbtNumberClass = NBTTagInt::class.java.superclass

        val e = nbtNumberClass.getDeclaredMethod("e")
        e.isAccessible = true
        numberE = lookup.unreflect(e)

        val asDouble = nbtNumberClass.getDeclaredMethod("asDouble")
        asDouble.isAccessible = true
        numberAsDouble = lookup.unreflect(asDouble)
    }

    override fun read(item: ItemStack): Any {
        if(item is CraftItemStack)
            return (((itemHandleGetter(item) ?: return emptyMap<String, NBTBase>()) as net.minecraft.server.v1_12_R1.ItemStack).tag
                ?: return emptyMap<String, NBTBase>()).map
        return unhandledTagsGetter(itemMetaGetter(item) ?: return emptyMap<String, NBTBase>())
    }

    override fun write(item: ItemStack): Any {
        if(item is CraftItemStack){
            val nms = itemHandleGetter(item) as? net.minecraft.server.v1_12_R1.ItemStack ?: return HashMap<String, NBTBase>()
            var tag = nms.tag
            if(tag == null){
                tag = NBTTagCompound()
                nms.tag = tag
            }
            return tag.map
        }
        var meta = itemMetaGetter(item)
        if(meta == null){
            meta = item.itemMeta
            itemMetaSetter(item, meta)
        }
        return unhandledTagsGetter(meta)
    }

    override fun clone(nbt: Any): Any {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val new = HashMap<String, NBTBase>(nbt.size + 1)
        nbt.forEach{if(it.key != "ench" && it.key != "display")new[it.key] = it.value.clone()}
        return new
    }

    override fun writeTo(nbt: Any, item: ItemStack) {
        @Suppress("UNCHECKED_CAST")
        val writeTo = write(item) as MutableMap<String, NBTBase>
        @Suppress("UNCHECKED_CAST")
        (nbt as Map<out String, NBTBase>).forEach{
            writeTo[it.key] = it.value.clone()
        }
    }

    override fun has(nbt: Any, key: String): Boolean{
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        return nbt.containsKey(key)
    }

    override fun remove(nbt: Any, key: String): Boolean {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        return nbt.remove(key) != null
    }

    override fun new(): Any = HashMap<String, NBTBase>(2)

    override fun string(nbt: Any, key: String, default: String?): String? {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val base = nbt[key] ?: default
        if(base !is NBTTagString)return default
        return base.c_()
    }

    override fun int(nbt: Any, key: String, default: Int): Int {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val base = nbt[key] ?: default
        if(base::class.java.superclass != nbtNumberClass)return default
        return numberE(base) as Int
    }

    override fun double(nbt: Any, key: String, default: Double): Double {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val base = nbt[key] ?: default
        if(base::class.java.superclass != nbtNumberClass)return default
        return numberAsDouble(base) as Double
    }

    override fun setString(nbt: Any, key: String, value: String) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagString(value)
    }

    override fun setInt(nbt: Any, key: String, value: Int) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagInt(value)
    }

    override fun setDouble(nbt: Any, key: String, value: Double) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagDouble(value)
    }

    override fun toJson(nbt: Any): JsonObject{
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        val result = HashMap<String, JsonElement>()
        fun encode(value: NBTBase): JsonElement?{
            return when(value){
                is NBTTagInt -> JsonPrimitive(value.e())
                is NBTTagDouble -> JsonPrimitive(value.asDouble())
                is NBTTagString -> JsonPrimitive(value.c_())
                is NBTTagCompound -> toJson(value.map)
                is NBTTagList -> {
                    val list = ArrayList<JsonElement>()
                    value.list.forEach{encode(it).nonNull(list::add)}
                    JsonArray(list)
                }
                else -> null
            }
        }
        nbt.forEach{
            val key = it.key
            val value = it.value
            if(key == "ench" || key == "display")return@forEach
            encode(value).nonNull{v -> result[key] = v}
        }
        return JsonObject(result)
    }

    override fun fromJson(json: JsonObject): Any{
        val result = HashMap<String, NBTBase>()
        fun decode(value: JsonElement): NBTBase?{
            if(value is JsonObject){
                val compound = NBTTagCompound()
                @Suppress("UNCHECKED_CAST")
                compound.map.putAll(fromJson(value) as HashMap<String, NBTBase>)
                return compound
            }
            if(value is JsonArray){
                val array = NBTTagList()
                value.forEach{array.add(decode(it))}
                return array
            }
            if(value !is JsonPrimitive)return null
            if(value.isString) return NBTTagString(value.content)
            value.intOrNull.nonNull{i -> return NBTTagInt(i)}
            value.doubleOrNull.nonNull{d -> return NBTTagDouble(d)}
            return null
        }
        json.forEach{
            val key = it.key
            val value = it.value
            decode(value).nonNull{v -> result[key] = v}
        }
        return result
    }
}