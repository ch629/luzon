package com.luzon.runtime

object GlobalFunctionManager {
    private val functions = hashMapOf<String, LzFunction>()

    fun invokeFunction(name: String, args: List<LzObject>): LzObject? {
        // TODO: Should this environment be global?
        return functions[LzFunction.getFunctionSignature(name, args)]?.invoke(Environment.global, args)
    }

    fun registerFunction(function: LzFunction) {
        functions += function.getSignatureString() to function
        println(function.getSignatureString())
    }

    fun registerFunction(name: String, function: LzFunction) {
        functions += name to function
    }

    operator fun plusAssign(func: LzFunction) = registerFunction(func)
    operator fun set(name: String, func: LzFunction) = registerFunction(name, func) // TODO: Do I need this one?
    operator fun invoke(name: String, args: List<LzObject>) = invokeFunction(name, args)
}