package pluginloader.internal.bukkit.test

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import pluginloader.api.Item
import pluginloader.internal.bukkit.nms.v1_12_R1.check_1_12_R1

class Serialization{
    private val debug = true

    @ExperimentalSerializationApi
    @InternalSerializationApi
    @Test
    fun item(){
        check_1_12_R1()
        val item = Item.default()
        item["lol"] = "lol"
        test(item)
    }

    @ExperimentalSerializationApi
    @InternalSerializationApi
    fun test(test: Any){
        @Suppress("UNCHECKED_CAST")
        val serializer = test::class.serializer() as KSerializer<Any>
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
        if(debug) println("[Yaml] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
    }
}