package pluginloader.internal.bukkit.nms.v1_16_R2

import kotlinx.serialization.json.*
import net.minecraft.server.v1_16_R2.*
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import pluginloader.api.bukkit.NBT
import pluginloader.api.nonNull
import pluginloader.internal.shared.getClass
import pluginloader.internal.shared.unsetFinal

internal object NBT: NBT.AbstractNBT {
    private val tags = getClass("org.bukkit.craftbukkit.v1_16_R2.inventory.CraftMetaItem").getDeclaredField("unhandledTags").unsetFinal()
    private val meta = getClass("org.bukkit.inventory.ItemStack").getDeclaredField("meta").unsetFinal()

    override fun read(item: ItemStack): Any {
        if(item is CraftItemStack)
            return ((item.handle ?: return emptyMap<String, NBTBase>()).tag
                    ?: return emptyMap<String, NBTBase>()).map
        return tags.get(meta.get(item) ?: return emptyMap<String, NBTBase>())
    }

    override fun write(item: ItemStack): Any {
        if(item is CraftItemStack){
            val nms = item.handle ?: return HashMap<String, NBTBase>()
            var tag = nms.tag
            if(tag == null){
                tag = NBTTagCompound()
                nms.tag = tag
            }
            return tag.map
        }
        var meta = meta.get(item)
        if(meta == null){
            meta = item.itemMeta
            pluginloader.internal.bukkit.nms.v1_16_R2.NBT.meta.set(item, meta)
        }
        return tags.get(meta)
    }

    override fun clone(nbt: Any): Any {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val new = HashMap<String, NBTBase>(nbt.size + 1)
        nbt.forEach{new[it.key] = it.value.clone()}
        return new
    }

    override fun writeTo(nbt: Any, item: ItemStack) {
        @Suppress("UNCHECKED_CAST")
        val writeTo = write(item) as MutableMap<String, NBTBase>
        @Suppress("UNCHECKED_CAST")
        (nbt as Map<out String, NBTBase>).forEach{if(it.key != "ench" && it.key != "display")writeTo[it.key] = it.value.clone()}
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
        return base.asString()
    }

    override fun int(nbt: Any, key: String, default: Int): Int {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val base = nbt[key] ?: default
        if(base !is NBTTagInt)return default
        return base.asInt()
    }

    override fun double(nbt: Any, key: String, default: Double): Double {
        @Suppress("UNCHECKED_CAST")
        nbt as Map<String, NBTBase>
        val base = nbt[key] ?: default
        if(base !is NBTTagDouble)return default
        return base.asDouble()
    }

    override fun setString(nbt: Any, key: String, value: String) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagString.a(value)
    }

    override fun setInt(nbt: Any, key: String, value: Int) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagInt.a(value)
    }

    override fun setDouble(nbt: Any, key: String, value: Double) {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        nbt[key] = NBTTagDouble.a(value)
    }

    override fun toJson(nbt: Any): JsonObject {
        @Suppress("UNCHECKED_CAST")
        nbt as HashMap<String, NBTBase>
        val result = HashMap<String, JsonElement>()
        nbt.forEach{
            val key = it.key
            val value = it.value
            if(key == "ench" || key == "display")return@forEach
            when(value){
                is NBTTagInt -> result[key] = JsonPrimitive(value.asInt())
                is NBTTagDouble -> result[key] = JsonPrimitive(value.asDouble())
                is NBTTagString -> result[key] = JsonPrimitive(value.asString())
            }
        }
        return JsonObject(result)
    }

    override fun fromJson(json: JsonObject): Any{
        val result = HashMap<String, NBTBase>()
        json.forEach{
            val key = it.key
            val value = it.value
            if(value !is JsonPrimitive)return@forEach
            if(value.isString){
                result[key] = NBTTagString.a(value.content)
                return@forEach
            }
            value.intOrNull.nonNull {i ->
                result[key] = NBTTagInt.a(i)
                return@forEach
            }
            value.doubleOrNull.nonNull { d ->
                result[key] = NBTTagDouble.a(d)
                return@forEach
            }
        }
        return result
    }
}