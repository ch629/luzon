package com.luzon.runtime

object EnvironmentManager {
    var currentEnvironment = Environment.global
        private set

    fun pop(): Environment = currentEnvironment.apply {
        currentEnvironment = currentEnvironment.pop()
    }

    fun newEnvironment(): Environment = currentEnvironment.apply {
        currentEnvironment = currentEnvironment.newEnv()
    }

    operator fun plusAssign(pair: Pair<String, LzObject>) = currentEnvironment.plusAssign(pair)
    operator fun get(name: String) = currentEnvironment[name]

    operator fun set(name: String, value: LzObject) {
        currentEnvironment[name] = value
    }
}