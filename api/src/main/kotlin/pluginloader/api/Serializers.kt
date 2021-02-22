package pluginloader.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.*

object UUIDSerializer: KSerializer<UUID> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UUID") {
        element<Long>("least")
        element<Long>("most")
    }

    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.leastSignificantBits)
            encodeLongElement(descriptor, 1, value.mostSignificantBits)
        }

    override fun deserialize(decoder: Decoder): UUID =
        decoder.decodeStructure(descriptor) {
            var most = 0L
            var least = 0L
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> least = decodeLongElement(descriptor, 0)
                    1 -> most = decodeLongElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            UUID(most, least)
        }
}
