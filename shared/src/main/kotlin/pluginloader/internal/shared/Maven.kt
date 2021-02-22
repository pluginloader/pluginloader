package pluginloader.internal.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import javax.xml.stream.XMLInputFactory

internal class Maven(private val url: String){
    fun download(dir: String, group: String, artifact: String, version: String?){
        if(File(dir, "${group.replace(".", "/")}/$artifact").exists())return
        val artfs = HashMap<Pair<String, String>, MavenArtifactMetadata>()
        fun get(group: String, artifact: String, version: String?){
            if(group.contains("org.jetbrains")) {
                when(artifact){
                    "kotlinx-serialization-core-jvm", "kotlinx-serialization-json-jvm", "kotlin-stdlib-jdk8" -> return
                }
            }
            if(artfs[group to artifact] != null)return
            val getVersion = version ?: getInfo(group, artifact).release
            val art = getArtifactInfo(group, artifact, getVersion)
            artfs[group to artifact] = art
            art.dependencies.forEach{
                get(if(it.group == "\${project.groupId}") group else it.group, it.artifact, if(it.version == "\${project.version}") version else it.version)
            }
        }
        get(group, artifact, version)
        artfs.forEach{
            val artMeta = it.value
            val file = File(dir, it.key.first.replace(".", "/") + "/" + artMeta.artifact + "/" + artMeta.version + ".jar")
            val dependency = File(dir, it.key.first.replace(".", "/") + "/" + artMeta.artifact + "/" + artMeta.version + "-dependency.json")
            if(file.exists())return@forEach
            file.parentFile.mkdirs()
            file.createNewFile()
            dependency.writeText(Json.encodeToString(artMeta.dependencies))
            file.writeBytes(getArtifact(artMeta.group, artMeta.artifact, artMeta.version))
        }
    }

    fun getInfo(group: String, artifact: String): MavenMetadata{
        val metadataRaw = URL("${urlOf(group, artifact)}/maven-metadata.xml").readText()
        val xml = XMLInputFactory.newInstance().createXMLEventReader(ByteArrayInputStream(metadataRaw.toByteArray()))
        val versions = ArrayList<String>()
        var latest: String? = null
        var release: String? = null
        while (xml.hasNext()){
            val tag = xml.nextTag()
            val value = xml.next()
            if(!tag.isStartElement)continue
            when(tag.asStartElement().name.localPart){
                "version" -> versions.add(value.toString())
                "latest" -> latest = value.toString()
                "release" -> release = value.toString()
            }
        }
        xml.close()

        return MavenMetadata(group, artifact, latest ?: versions[versions.size - 1], release ?: versions[versions.size - 1], versions)
    }

    fun getArtifactInfo(group: String, artifact: String, version: String): MavenArtifactMetadata{
        val metadataRaw = URL("${urlOf(group, artifact)}/$version/$artifact-$version.pom").readText()
        val xml = XMLInputFactory.newInstance().createXMLEventReader(ByteArrayInputStream(metadataRaw.toByteArray()))
        val dependencies = ArrayList<MavenArtifactDependency>()
        var dependencyCheck = false
        var dependencyGroup: String? = null
        var dependencyArtifact: String? = null
        var dependencyVersion: String? = null
        while (xml.hasNext()){
            val tag = xml.nextTag()
            val value = xml.next()
            if(!tag.isStartElement)continue
            val name = tag.asStartElement().name.localPart
            if(name == "build")break
            if(name == "dependencies"){
                dependencyCheck = true
                continue
            }
            if(!dependencyCheck)continue
            when(name){
                "dependency" -> {
                    if(dependencyGroup != null && dependencyArtifact != null && dependencyVersion != null)
                        dependencies.add(MavenArtifactDependency(dependencyGroup, dependencyArtifact, dependencyVersion))
                    dependencyGroup = null
                    dependencyArtifact = null
                    dependencyVersion = null
                }
                "groupId" -> dependencyGroup = value.toString()
                "artifactId" -> dependencyArtifact = value.toString()
                "version" -> dependencyVersion = value.toString()
                "scope" -> if(value.toString() != "runtime"){
                    dependencyGroup = null
                    dependencyArtifact = null
                    dependencyVersion = null
                }
                else -> {
                    dependencyGroup = null
                    dependencyArtifact = null
                    dependencyVersion = null
                }
            }
        }
        if(dependencyGroup != null && dependencyArtifact != null && dependencyVersion != null)
            dependencies.add(MavenArtifactDependency(dependencyGroup, dependencyArtifact, dependencyVersion))
        xml.close()
        return MavenArtifactMetadata(group, artifact, version, dependencies)
    }

    fun getArtifact(group: String, artifact: String, version: String): ByteArray{
        return URL("${urlOf(group, artifact)}/$version/$artifact-$version.jar").readBytes()
    }

    private fun urlOf(group: String, artifact: String) = "$url/${group.replace('.', '/')}/${artifact.replace('.', '/')}"

    companion object{
        val central = Maven("https://repo1.maven.org/maven2")
    }
}

internal data class MavenMetadata(val group: String, val artifact: String, val latest: String, val release: String, val versions: List<String>)

internal data class MavenArtifactMetadata(val group: String, val artifact: String, val version: String, val dependencies: List<MavenArtifactDependency>)

@Serializable
internal data class MavenArtifactDependency(val group: String, val artifact: String, val version: String)