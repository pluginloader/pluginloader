package pluginloader.internal.bukkit.nms.v1_12_R1

import pluginloader.api.bukkit.NBT

internal fun check_1_12_R1(): Boolean{
    return try{
        org.bukkit.craftbukkit.v1_12_R1.CraftServer::class.java.name
        NBT.AbstractNBT.provider = pluginloader.internal.bukkit.nms.v1_12_R1.NBT
        true
    }catch (ex: Throwable){
        false
    }
}