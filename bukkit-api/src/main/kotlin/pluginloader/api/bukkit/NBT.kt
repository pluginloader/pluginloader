package pluginloader.api.bukkit

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import org.bukkit.inventory.ItemStack

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
inline class NBT private constructor(private val nbt: Any){
    companion object{
        fun read(item: ItemStack) = NBT(AbstractNBT.provider.read(item))
        fun write(item: ItemStack) = NBT(AbstractNBT.provider.write(item))
        fun clone(item: ItemStack) = NBT(AbstractNBT.provider.clone(AbstractNBT.provider.write(item)))
        fun new() = NBT(AbstractNBT.provider.new())

        fun has(item: ItemStack, key: String) = read(item).has(key)
        fun remove(item: ItemStack, key: String) = write(item).remove(key)

        fun string(item: ItemStack, key: String, default: String? = null) = read(item).string(key, default)
        fun int(item: ItemStack, key: String, default: Int = -1) = read(item).int(key, default)
        fun double(item: ItemStack, key: String, default: Double = -1.0) = read(item).double(key, default)

        fun setString(item: ItemStack, key: String, value: String) = write(item).setString(key, value)
        fun setInt(item: ItemStack, key: String, value: Int) = write(item).setInt(key, value)
        fun setDouble(item: ItemStack, key: String, value: Double) = write(item).setDouble(key, value)

        @Deprecated("do not use this lol")
        fun fromJson(json: JsonObject) = AbstractNBT.provider.fromJson(json)

        fun fromJsonNBT(json: JsonObject): NBT = NBT(AbstractNBT.provider.fromJson(json))
    }

    fun has(key: String) = AbstractNBT.provider.has(nbt, key)
    fun remove(key: String) = AbstractNBT.provider.remove(nbt, key)
    fun clone() = NBT(AbstractNBT.provider.clone(nbt))
    fun writeTo(item: ItemStack) = AbstractNBT.provider.writeTo(nbt, item)

    fun string(key: String, default: String?) = AbstractNBT.provider.string(nbt, key, default)
    fun int(key: String, default: Int) = AbstractNBT.provider.int(nbt, key, default)
    fun double(key: String, default: Double) = AbstractNBT.provider.double(nbt, key, default)

    fun setString(key: String, value: String) = AbstractNBT.provider.setString(nbt, key, value)
    fun setInt(key: String, value: Int) = AbstractNBT.provider.setInt(nbt, key, value)
    fun setDouble(key: String, value: Double) = AbstractNBT.provider.setDouble(nbt, key, value)

    fun toJson(): JsonObject = AbstractNBT.provider.toJson(nbt)

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
            encoder.encodeString(Json.encodeToString(JsonObject.serializer(), AbstractNBT.provider.toJson(value.nbt)))
        }
    }
}