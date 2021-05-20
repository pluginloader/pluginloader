import pluginloader.internal.shared.Maven

fun main() {
    Maven.download(Maven.central, "delete_lol", "org.iq80.leveldb", "leveldb", "0.12")
}