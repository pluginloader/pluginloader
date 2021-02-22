package pluginloader.internal.bukkit.nms.v1_16_R2

import pluginloader.api.bukkit.NBT

internal fun check_1_16_R2(): Boolean{
    return try{
        org.bukkit.craftbukkit.v1_16_R2.CraftServer::class.java.name
        NBT.AbstractNBT.provider = pluginloader.internal.bukkit.nms.v1_16_R2.NBT
        true
    }catch (ex: Throwable){
        false
    }
}