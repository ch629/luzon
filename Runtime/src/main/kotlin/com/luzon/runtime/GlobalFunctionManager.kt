package com.luzon.runtime

object GlobalFunctionManager {
    private val functions = hashMapOf<String, LzFunction>()

    fun invokeFunction(name: String, args: List<LzObject>): LzObject? {
        // TODO: Should this environment be global?
        return functions[getFunctionMapName(name, args)]?.invoke(Environment.global, args)
    }

    fun registerFunction(name: String, function: LzFunction) {
        functions += function.params.toFunctionMapName(name) to function
    }

    operator fun plusAssign(pair: Pair<String, LzFunction>) = registerFunction(pair.first, pair.second)
    operator fun set(name: String, func: LzFunction) = registerFunction(name, func)
    operator fun invoke(name: String, args: List<LzObject>) = invokeFunction(name, args)
}