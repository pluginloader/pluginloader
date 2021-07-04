package pluginloader.api

inline fun caching(func: () -> Unit): Throwable?{
    return try{
        func()
        null
    }catch (ex: Throwable){
        ex
    }
}

inline fun <T> T?.nonNull(func: (T) -> Unit){
    func(this ?: return)
}