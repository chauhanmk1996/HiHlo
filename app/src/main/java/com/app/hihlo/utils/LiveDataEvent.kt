package com.app.hihlo.utils

class LiveDataEvent<T>(private val content: T) {
    private var hasBeenHandled = false

    val contentIfNotHandled: T?
        get() {
            if (hasBeenHandled) {
                return null
            }
            hasBeenHandled = true
            return content
        }

    fun peekContent(): T {
        return content
    }
}