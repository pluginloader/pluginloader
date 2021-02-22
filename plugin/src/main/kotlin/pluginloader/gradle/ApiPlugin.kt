package pluginloader.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class Config(private val project: Project){
    fun plu(vararg plugins: String){
        plugins.forEach{project.dependencies.add("dependency", "pluginloader:$it")}
    }
}

class ApiPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.getByPath("jar").doFirst{_ ->
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