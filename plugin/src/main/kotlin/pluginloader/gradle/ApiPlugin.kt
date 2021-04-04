package pluginloader.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class Config(private val project: Project){
    fun plu(vararg plugins: String){
        plugins.forEach{project.dependencies.add("dependency", "pluginloader:${
            if(!it.contains(':')) "$it:1.0.0" else it
        }")}
    }

    fun lib(vararg plugins: String){
        plugins.forEach{project.dependencies.add("compileOnly", "pluginloader:${
            if(!it.contains(':')) {
                "$it:1.0.0"
            } else {
                val split = it.split(":")
                val ver = split[1]
                if (ver.startsWith(".")) {
                    "${split[0]}:1.0$ver"
                } else {
                    "${split[0]}:$ver"
                }
            }
        }")}
    }
}

class ApiPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.getByPath("jar").doFirst{_ ->
            val dir = project.file("build/classes/kotlin/main/pluginloader")
            if(dir.exists())dir.deleteRecursively()
            project.configurations.getByName("dependency").allDependencies.forEach{
                val f = project.file("build/classes/kotlin/main/pluginloader/${it.name}.dependency")
                f.parentFile.mkdirs()
                f.createNewFile()
            }
            project.configurations.getByName("mavenDependency").allDependencies.forEach{
                val f = project.file("build/classes/kotlin/main/pluginloader/${it.group};${it.name};${it.version}.mavenDependency")
                f.parentFile.mkdirs()
                f.createNewFile()
            }
        }

        project.repositories.add(project.repositories.mavenLocal())
        project.repositories.add(project.repositories.mavenCentral())

        val config = project.configurations.create("dependency")
        config.isTransitive = false
        project.configurations.getByName("compileClasspath").extendsFrom(config)

        project.extensions.create("plu", Config::class.java, project)

        project.dependencies.add("compileOnly", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.properties["kotlinVersion"]}")
        project.dependencies.add("compileOnly", "org.jetbrains.kotlinx:kotlinx-serialization-core:${project.properties["kotlinSerializationVersion"]}")
        project.dependencies.add("compileOnly", "org.jetbrains.kotlinx:kotlinx-serialization-json:${project.properties["kotlinSerializationVersion"]}")
        project.dependencies.add("compileOnly", "pluginloader:api:${project.properties["pluginloaderVersion"]}")

        val mvnDependency = project.configurations.create("mavenDependency")
        mvnDependency.isTransitive = false
        project.configurations.getByName("compileClasspath").extendsFrom(mvnDependency)
    }
}