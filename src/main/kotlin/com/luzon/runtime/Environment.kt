package com.luzon.runtime

import com.luzon.exceptions.SymbolExistsException
import com.luzon.exceptions.UnknownFunctionException
import com.luzon.exceptions.UnknownSymbolException

class Environment private constructor(private val parent: Environment?) {
    private val variables = hashMapOf<String, LzVariable>()
    private val functions = hashMapOf<String, MutableSet<LzFunction>>()

    companion object {
        val global = Environment(null)
    }

    fun reset() {
        variables.clear()
        functions.clear()
    }

    val global: Boolean
        get() = parent == null

    @Throws(UnknownSymbolException::class)
    fun findVariable(name: String): LzVariable = when {
        variables.containsKey(name) -> variables[name]!!
        parent != null -> parent.findVariable(name)
        else -> throw UnknownSymbolException(name)
    }

    @Throws(UnknownFunctionException::class)
    fun findFunction(name: String, args: List<LzObject>): LzFunction {
        val functions = functions[name]

        if (functions != null && functions.isNotEmpty()) {
            val function = functions.find { it.argumentsMatchParams(args) }

            if (function != null)
                return function
        }

        return parent?.findFunction(name, args) ?: throw UnknownFunctionException(name, args)
    }

    @Throws(UnknownFunctionException::class)
    fun invokeFunction(name: String, args: List<LzObject> = emptyList()) =
        findFunction(name, args).invoke(this, args)

    @Throws(UnknownSymbolException::class)
    operator fun get(name: String) = findVariable(name).value

    operator fun set(name: String, value: LzObject) = setVariable(name, value)

    operator fun plusAssign(pair: Pair<String, LzObject>) {
        defineVariable(pair.first, pair.second)
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

    @Throws(SymbolExistsException::class)
    fun defineVariable(name: String, value: LzObject) {
        if (variables.contains(name))
            throw SymbolExistsException(name)
        variables[name] = LzVariable(name, value, value.clazz, false)
    }

    fun setVariable(name: String, value: LzObject) {
        findVariable(name).assign(value)
    }

    fun copy(): Environment {
        val newEnvironment = Environment(parent)
        newEnvironment.variables.putAll(variables)
        newEnvironment.functions.putAll(functions)
        return newEnvironment
    }
}
