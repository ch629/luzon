package com.luzon.runtime

import java.util.*

object EnvironmentManager {
    private val stack = Stack<Environment>()

    val currentEnvironment: Environment
        get() = stack.peek()

    init {
        stack.push(Environment.global)
    }

    fun push(environment: Environment) {
        stack.push(environment)
    }

    fun pop(): Environment = if (stack.size > 1) stack.pop() else currentEnvironment.pop()

    fun newEnvironment(): Environment = currentEnvironment.newEnv().apply {
        stack.push(this)
    }

    operator fun plusAssign(pair: Pair<String, LzObject>) = currentEnvironment.plusAssign(pair)
    operator fun plusAssign(func: LzFunction) = currentEnvironment.plusAssign(func)
    operator fun get(name: String) = currentEnvironment[name]
    operator fun invoke(name: String, args: List<LzObject>) = currentEnvironment.invokeFunction(name, args)

    operator fun set(name: String, value: LzObject) {
        currentEnvironment[name] = value
    }
}

fun withNewEnvironment(block: () -> Unit) {
    EnvironmentManager.newEnvironment()
    block()
    EnvironmentManager.pop()
}

fun withEnvironment(environment: Environment, block: () -> Unit) {
    EnvironmentManager.push(environment)
    block()
    EnvironmentManager.pop()
}