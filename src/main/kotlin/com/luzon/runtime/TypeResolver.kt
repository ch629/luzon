package com.luzon.runtime

object TypeResolver {
    private val map = hashMapOf<String, LzType<*>>()

    init {
        initPrimitives()
    }

    private fun initPrimitives() {
        registerType("Int", LzInt)
        registerType("Float", LzFloat)
        registerType("Double", LzDouble)
        registerType("Boolean", LzBoolean)
        registerType("String", LzString)
    }

    operator fun plusAssign(pair: Pair<String, LzType<*>>) = registerType(pair.first, pair.second)
    operator fun get(name: String) = findType(name)

    fun registerType(name: String, type: LzType<*>) {
        map[name] = type // TODO: Error if exists
    }

    fun findType(name: String) = map[name]
}