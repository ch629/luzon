package com.luzon.runtime

import java.util.*

object EnvironmentManager {
    private val stack = Stack<Environment>()

    val currentEnvironment: Environment
        get() = stack.peek()

    fun push(environment: Environment) {
        stack.push(environment)
    }

    fun pop(): Environment = if (stack.size > 1) stack.pop() else currentEnvironment.pop()

    fun newEnvironment(): Environment = currentEnvironment.newEnv().apply {
        stack.push(this)
    }

    operator fun plusAssign(pair: Pair<String, LzObject>) = currentEnvironment.plusAssign(pair)
    operator fun get(name: String) = currentEnvironment[name]

    operator fun set(name: String, value: LzObject) {
        currentEnvironment[name] = value
    }
}

fun with(environment: Environment, block: () -> Unit) {
    EnvironmentManager.push(environment)
    block()
    EnvironmentManager.pop()
}