package pluginloader.api

import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class JsonInJson(var json: String? = null)

@InternalSerializationApi
class Serializers {
    private val debug = false

    @Test
    fun jsonInJsonInJson(){//testing 1.2.0 bug
        if(true)return
        val cycles = 15
        val threads = 10
        val completed = AtomicInteger(0)
        repeat(threads) {
            Thread {
                var json = Json.encodeToString(JsonInJson.serializer(), JsonInJson("lol"))
                repeat(cycles) {
                    json = Json.encodeToString(JsonInJson.serializer(), JsonInJson(json))
                }
                var decoded = Json.decodeFromString(JsonInJson.serializer(), json)
                repeat(cycles) {
                    decoded = Json.decodeFromString(JsonInJson.serializer(), decoded.json!!)
                }
                if (decoded.json != "lol") throw Error("lol")
                completed.getAndIncrement()
            }.start()
        }
        while (completed.get() != threads) Thread.sleep(10)
    }

    @ExperimentalSerializationApi
    @Suppress("UNCHECKED_CAST")
    @Test
    fun uuid(){
        test(UUID.randomUUID(), UUIDSerializer as KSerializer<Any>)
        test(UUID.randomUUID(), UUIDStringSerializer as KSerializer<Any>)
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
        /*encoded = Yaml.default.encodeToString(serializer, test)
        decoded = Yaml.default.decodeFromString(serializer, encoded)
        if(debug) println("[Yaml] Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)*/
    }
}