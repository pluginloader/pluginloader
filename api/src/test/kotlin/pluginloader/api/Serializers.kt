package pluginloader.api

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import java.util.*

@InternalSerializationApi
class Serializers {
    private val debug = false

    @ExperimentalSerializationApi
    @Suppress("UNCHECKED_CAST")
    @Test
    fun uuid(){
        test(UUID.randomUUID(), UUIDSerializer as KSerializer<Any>)
    }

    @ExperimentalSerializationApi
    fun test(test: Any, s: KSerializer<Any>?){
        @Suppress("UNCHECKED_CAST")
        val serializer = s ?: test::class.serializer() as KSerializer<Any>
        var encoded = Json.encodeToString(serializer, test)
        var decoded = Json.decodeFromString(serializer, encoded)
        if(debug) println("[Json] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
        encoded = Cbor.encodeToHexString(serializer, test)
        decoded = Cbor.decodeFromHexString(serializer, encoded)
        if(debug) println("[Cbor] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
        encoded = ProtoBuf.encodeToHexString(serializer, test)
        decoded = ProtoBuf.decodeFromHexString(serializer, encoded)
        if(debug) println("[ProtoBuf] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
        encoded = Yaml.default.encodeToString(serializer, test)
        decoded = Yaml.default.decodeFromString(serializer, encoded)
        if(debug) println("[ProtoBuf] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
    }
}