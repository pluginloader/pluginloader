package pluginloader.internal.bukkit.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import pluginloader.api.bukkit.NBT
import pluginloader.internal.bukkit.nms.v1_12_R1.check_1_12_R1

fun main() {
    check_1_12_R1()
    val nbt = NBT.fromJsonNBT(Json.decodeFromString(JsonObject.serializer(), "{\"Display\":{\"color\":\"kek\"}}"))
    val toJson = nbt.toJson()
    println(toJson)
    val decoded = NBT.fromJsonNBT(toJson)
    println(decoded)
}