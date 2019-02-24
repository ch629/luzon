package com.luzon.runtime

object EnvironmentManager {
    private var currentEnvironment = Environment.global

    fun pop() {
        currentEnvironment = currentEnvironment.pop()
    }

    fun newEnvironment() {
        currentEnvironment = currentEnvironment.newEnv()
    }

    operator fun plusAssign(pair: Pair<String, LzObject>) = currentEnvironment.plusAssign(pair)
    operator fun get(name: String) = currentEnvironment[name]

    operator fun set(name: String, value: LzObject) {
        currentEnvironment[name] = value
    }
}