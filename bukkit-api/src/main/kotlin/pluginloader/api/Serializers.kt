package pluginloader.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

object EnchantmentSerializer: KSerializer<Enchantment>{
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("enchantment", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Enchantment = Enchantment.getByName(decoder.decodeString().uppercase())

    override fun serialize(encoder: Encoder, value: Enchantment) = encoder.encodeString(value.name)
}

object ItemStackSerializer: KSerializer<ItemStack>{
    override val descriptor: SerialDescriptor = Item.serializer().descriptor

    override fun deserialize(decoder: Decoder): ItemStack = Item.serializer().deserialize(decoder).item()

    override fun serialize(encoder: Encoder, value: ItemStack) = Item.serializer().serialize(encoder, Item.item(value))
}