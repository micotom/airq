package com.funglejunk.airq.model

data class StreamResult<V : Any>(val info: String, val success: Boolean, val content: V) {

    inline fun <W : Any> map(default: W, f: (StreamResult<V>) -> StreamResult<W>): StreamResult<W> {
        return when (success) {
            true -> f(this)
            false -> StreamResult(info = info, success = false, content = default)
        }
    }

    inline fun <T : Any> fmap(default: T, f: (StreamResult<V>) -> T): T {
        return when (success) {
            true -> f(this)
            false -> default
        }
    }

}