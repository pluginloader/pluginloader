dependencies {
    api(project(":shared"))
}

tasks.withType<Jar>{
    archiveFileName.set("${rootProject.name}-${rootProject.version}-standalone.jar")
    this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get().map{if(it.isDirectory) it else zipTree(it)})
    manifest.attributes["Main-Class"] = "pluginloader.internal.standalone.StartKt"
}