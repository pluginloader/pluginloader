package pluginloader.api.bukkit

import de.tr7zw.changeme.nbtapi.NBTCompound
import de.tr7zw.changeme.nbtapi.NBTItem
import de.tr7zw.changeme.nbtapi.NBTType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import pluginloader.api.nonNull

@JvmInline
@Serializable(NBT.Serializer::class)
value class NBT private constructor(private val nbt: Any){
    companion object{
        fun ItemStack.readNBT(): NBT = read(this)
        fun ItemStack.writeNBT(): NBT = write(this)

        fun read(item: ItemStack): NBT = NBT(NBTItem(item))
        fun write(item: ItemStack): NBT = NBT(NBTItem(item, true))
        fun clone(item: ItemStack): NBT = NBT(NBTItem(item))
        fun new(): NBT = NBT(NBTItem(ItemStack(Material.STONE)))

        fun has(item: ItemStack, key: String) = read(item).has(key)
        fun remove(item: ItemStack, key: String) = write(item).remove(key)

        fun string(item: ItemStack, key: String, default: String? = null) = read(item).string(key, default)
        fun int(item: ItemStack, key: String, default: Int = -1) = read(item).int(key, default)
        fun double(item: ItemStack, key: String, default: Double = -1.0) = read(item).double(key, default)

        fun setString(item: ItemStack, key: String, value: String) = write(item).setString(key, value)
        fun setInt(item: ItemStack, key: String, value: Int) = write(item).setInt(key, value)
        fun setDouble(item: ItemStack, key: String, value: Double) = write(item).setDouble(key, value)

        internal fun fromJson(json: JsonObject): Any {
            val result = NBTItem(ItemStack(Material.STONE))
            fun decodeToNBT(compound: NBTCompound, json: JsonObject){
                json.forEach{
                    val key = it.key
                    val value = it.value
                    if(value is JsonObject){
                        decodeToNBT(compound.getOrCreateCompound(key), value)
                        return@forEach
                    }
                    if(value is JsonArray){
                        if(value.isEmpty())return@forEach
                        when(val first = value[0]){
                            is JsonObject -> {
                                val c = compound.getCompoundList(key)
                                value.forEach{v -> decodeToNBT(c.addCompound(), v as JsonObject)}
                            }
                            is JsonPrimitive -> {
                                if(first.isString){
                                    val c = compound.getStringList(key)
                                    value.forEach{v -> c.add((v as JsonPrimitive).content)}
                                }else{
                                    first.intOrNull.nonNull{
                                        val c = compound.getIntegerList(key)
                                        value.forEach{v -> c.add((v as JsonPrimitive).int)}
                                        return@forEach
                                    }
                                    first.doubleOrNull.nonNull {
                                        val c = compound.getDoubleList(key)
                                        value.forEach{v -> c.add((v as JsonPrimitive).double)}
                                        return@forEach
                                    }
                                }
                            }
                            else -> {}
                        }
                        return@forEach
                    }
                    if(value !is JsonPrimitive)return@forEach
                    if(value.isString){
                        compound.setString(key, value.content)
                        return@forEach
                    }
                    value.intOrNull.nonNull{i ->
                        compound.setInteger(key, i)
                        return@forEach
                    }
                    value.doubleOrNull.nonNull{d ->
                        compound.setDouble(key, d)
                        return@forEach
                    }
                }
            }
            decodeToNBT(result, json)
            return result
        }
        fun fromJsonNBT(json: JsonObject): NBT {
            return NBT(fromJson(json))
        }
    }

    fun has(key: String): Boolean = (nbt as NBTItem).hasKey(key)
    fun remove(key: String): Boolean {
        val hasKey = (nbt as NBTItem).hasKey(key)
        nbt.removeKey(key)
        return hasKey
    }
    fun clone(): NBT = NBT(NBTItem((nbt as NBTItem).item))
    fun writeTo(item: ItemStack) {
        (nbt as NBTItem).mergeCustomNBT(item)
    }

    fun string(key: String, default: String?): String? = (nbt as NBTItem).getString(key) ?: default
    fun int(key: String, default: Int): Int = if((nbt as NBTItem).hasKey(key)) nbt.getInteger(key) else default
    fun double(key: String, default: Double): Double = if((nbt as NBTItem).hasKey(key)) nbt.getDouble(key) else default

    fun setString(key: String, value: String): Unit = (nbt as NBTItem).setString(key, value)
    fun setInt(key: String, value: Int): Unit = (nbt as NBTItem).setInteger(key, value)
    fun setDouble(key: String, value: Double): Unit = (nbt as NBTItem).setDouble(key, value)

    fun toJson(): JsonObject {
        nbt as NBTItem
        fun encodeToObj(nbt: NBTCompound): JsonObject{
            val map = HashMap<String, JsonElement>()
            fun encode(key: String): JsonElement?{
                return when(nbt.getType(key)){
                    NBTType.NBTTagInt -> JsonPrimitive(nbt.getInteger(key))
                    NBTType.NBTTagDouble -> JsonPrimitive(nbt.getDouble(key))
                    NBTType.NBTTagString -> JsonPrimitive(nbt.getString(key))
                    NBTType.NBTTagCompound -> encodeToObj(nbt.getCompound(key))
                    NBTType.NBTTagList -> {
                        val list = ArrayList<JsonElement>()
                        when(nbt.getListType(key)){
                            NBTType.NBTTagCompound -> nbt.getCompoundList(key).forEach{list.add(encodeToObj(it))}
                            NBTType.NBTTagDouble -> nbt.getDoubleList(key).forEach{list.add(JsonPrimitive(it))}
                            NBTType.NBTTagFloat -> nbt.getFloatList(key).forEach{list.add(JsonPrimitive(it))}
                            NBTType.NBTTagInt -> nbt.getIntegerList(key).forEach{list.add(JsonPrimitive(it))}
                            NBTType.NBTTagLong -> nbt.getLongList(key).forEach{list.add(JsonPrimitive(it))}
                            NBTType.NBTTagString -> nbt.getStringList(key).forEach{list.add(JsonPrimitive(it))}

                            else -> {}
                        }
                        JsonArray(list)
                    }

                    else -> null
                }
            }
            nbt.keys.forEach{encode(it).nonNull{obj -> map[it] = obj}}
            return JsonObject(map)
        }
        return encodeToObj(nbt)
    }

    override fun toString(): String {
        return toJson().toString()
    }

    interface AbstractNBT {
        companion object{
            lateinit var provider: AbstractNBT
        }

        fun read(item: ItemStack): Any
        fun write(item: ItemStack): Any
        fun clone(nbt: Any): Any
        fun writeTo(nbt: Any, item: ItemStack)

        fun new(): Any

        fun has(nbt: Any, key: String): Boolean
        fun remove(nbt: Any, key: String): Boolean

        fun string(nbt: Any, key: String, default: String?): String?
        fun int(nbt: Any, key: String, default: Int): Int
        fun double(nbt: Any, key: String, default: Double): Double

        fun setString(nbt: Any, key: String, value: String)
        fun setInt(nbt: Any, key: String, value: Int)
        fun setDouble(nbt: Any, key: String, value: Double)

        fun toJson(nbt: Any): JsonObject
        fun fromJson(json: JsonObject): Any
    }

    object Serializer: KSerializer<Any>{
        override val descriptor: SerialDescriptor = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): Any {
            if(decoder is JsonDecoder) return fromJsonNBT(JsonObject.serializer().deserialize(decoder))
            return fromJsonNBT(Json.decodeFromString(JsonObject.serializer(), decoder.decodeString()))
        }

        override fun serialize(encoder: Encoder, value: Any) {
            value as NBT
            if(encoder is JsonEncoder) {
                JsonObject.serializer().serialize(encoder, (value).toJson())
                return
            }
            encoder.encodeString(Json.encodeToString(JsonObject.serializer(), value.toJson()))
        }
    }
}