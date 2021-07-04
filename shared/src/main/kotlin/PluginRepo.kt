package pluginloader.internal.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pluginloader.api.caching
import pluginloader.api.nonNull
import java.io.File
import java.net.URL

class PluginRepo(private val url: String){
    fun rawRead(read: String): ByteArray?{
        caching {
            val url = URL("$url/$read").openConnection()
            url.setRequestProperty("User-Agent", "pluginloader")
            url.connect()
            val bytes = url.getInputStream().readBytes()
            url.getInputStream().close()
            return bytes
        }
        return null
    }

    fun get(dir: File, group: String, plugin: String): File?{
        if(File(dir, "${group}/${plugin}.nonexists").exists())return null
        val jar = File(dir, "${group}/${plugin}.jar")
        if(jar.exists())return jar
        val base = baseURL(group, plugin)
        var metadata: String? = null
        caching{
            val url = URL("${base}/metadata.json").openConnection()
            url.setRequestProperty("User-Agent", "pluginloader")
            url.connect()
            metadata = url.getInputStream().readBytes().toString(Charsets.UTF_8)
            url.getInputStream().close()
        }.nonNull{
            println("url = '${"${base}/metadata.json"}'")
            throw it
        }
        if(metadata == null || metadata == "404" || metadata == "403"){
            val file = File(dir, "${group}/${plugin}.nonexists")
            file.parentFile.mkdirs()
            file.parentFile.mkdir()
            file.createNewFile()
            return null
        }
        val parsed = Json.decodeFromString(Metadata.serializer(), metadata!!)

        val url = URL("${base}/${parsed.count}.jar").openConnection()
        url.setRequestProperty("User-Agent", "pluginloader")
        url.connect()
        jar.parentFile.mkdirs()
        jar.parentFile.mkdir()
        jar.createNewFile()
        jar.writeBytes(url.getInputStream().readBytes())
        url.getInputStream().close()
        File(dir, "${group}/${plugin}.metadata.json").writeText(metadata!!)
        return jar
    }

    private fun baseURL(group: String, plugin: String): String{
        return "${url}/${group}/${plugin}"
    }

    companion object{
        val official = PluginRepo("https://plu.implario.dev/public")
    }
}

@Serializable
private class Metadata(val count: Int)