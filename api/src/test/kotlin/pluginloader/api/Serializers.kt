package pluginloader.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Test
import java.util.*

@InternalSerializationApi
class Serializers {
    private val debug = false

    @Test
    fun uuid(){
        @Suppress("UNCHECKED_CAST")
        test(UUID.randomUUID(), UUIDSerializer as KSerializer<Any>)
    }

    fun test(test: Any, s: KSerializer<Any>?){
        @Suppress("UNCHECKED_CAST")
        val serializer = s ?: test::class.serializer() as KSerializer<Any>
        val encoded = Json.encodeToString(serializer, test)
        val decoded = Json.decodeFromString(serializer, encoded)
        if(debug) println("Source: '$test', encoded: '$encoded', decoded '$decoded'")
        assert(test == decoded)
    }
}