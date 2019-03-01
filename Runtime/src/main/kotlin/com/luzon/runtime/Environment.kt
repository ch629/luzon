package com.luzon.runtime

class Environment private constructor(private val parent: Environment?) {
    private val values: HashMap<String, LzObject> = hashMapOf()

    companion object {
        val global = Environment(null)
    }

    fun isGlobal() = parent == null

    fun findValue(name: String): LzObject? = when {
        values.containsKey(name) -> values[name]!!
        parent != null -> parent.findValue(name)
        else -> null
    }

    operator fun get(name: String) = findValue(name)
    operator fun set(name: String, value: LzObject) = setValue(name, value)

    operator fun plusAssign(pair: Pair<String, LzObject>) {
        defineValue(pair.first, pair.second)
    }

    fun newEnv() = Environment(this)
    fun pop() = parent ?: this

    fun defineValue(name: String, value: LzObject) {
        if (values.contains(name))
            return // TODO: Error -> Already exists
        values[name] = value
    }

    fun setValue(name: String, value: LzObject) {
        if (!values.contains(name))
            return // TODO: Error -> Doesn't exist
        if (values[name]!!.type != value.type) // TODO: Custom Types, Constants, Subclass
            return // TODO: Error -> Wrong Type

        values[name] = value
    }

    fun copy(): Environment {
        val newEnvironment = Environment(parent)
        newEnvironment.values.putAll(values)
        return newEnvironment
    }
}