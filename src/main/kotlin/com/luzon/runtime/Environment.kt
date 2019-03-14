package com.luzon.runtime

class Environment private constructor(private val parent: Environment?) {
    private val values = hashMapOf<String, LzObject>()
    private val functions = hashMapOf<String, MutableSet<LzFunction>>()

    companion object {
        val global = Environment(null)
    }

    fun reset() {
        values.clear()
        functions.clear()
    }

    fun isGlobal() = parent == null

    fun findValue(name: String): LzObject? = when {
        values.containsKey(name) -> values[name]
        parent != null -> parent.findValue(name)
        else -> null
    }

    fun findFunction(name: String, args: List<LzObject>): LzFunction? {
        val functions = functions[name]

        if (functions != null && functions.isNotEmpty()) {
            val function = functions.find { it.argumentsMatchParams(args) }

            if (function != null)
                return function
        }

        return parent?.findFunction(name, args)
    }

    fun invokeFunction(name: String, args: List<LzObject>) =
            findFunction(name, args)?.invoke(this, args) ?: nullObject

    operator fun get(name: String) = findValue(name)
    operator fun set(name: String, value: LzObject) = setValue(name, value)

    operator fun plusAssign(pair: Pair<String, LzObject>) {
        defineValue(pair.first, pair.second)
    }

    operator fun plusAssign(function: LzFunction) = defineFunction(function)

    // TODO: This is a fine solution for now, but it will be better to hold the classes and find the functions within them using the current environment
    fun defineFunction(function: LzFunction) {
        val functions = functions[function.name] ?: mutableSetOf()
        functions.add(function)

        if (functions.size == 1)
            this.functions += function.name to functions
    }

    fun newEnv() = Environment(this)
    fun pop() = parent ?: this

    fun defineValue(name: String, value: LzObject) {
        if (values.contains(name))
            return // TODO: Error -> Already exists
        values[name] = value
    }

    fun setValue(name: String, value: LzObject) {
        when {
            values.contains(name) -> values[name] = value
            parent != null -> parent.setValue(name, value)
            else -> return // TODO: Value doesn't exist
        }
    }

    fun copy(): Environment {
        val newEnvironment = Environment(parent)
        newEnvironment.values.putAll(values)
        newEnvironment.functions.putAll(functions)
        return newEnvironment
    }
}