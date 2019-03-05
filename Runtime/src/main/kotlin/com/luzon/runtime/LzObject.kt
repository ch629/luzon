package com.luzon.runtime

fun primitiveObject(value: Any?) = LzObject(clazz = LzClass("PRIMITIVE"), value = value)

data class LzObject(val clazz: LzClass, val value: Any? = null, val environment: Environment = Environment.global) {
    operator fun get(name: String) = environment[name]

    fun invokeFunction(name: String, args: List<LzObject>) = clazz.invokeFunction(name, args, environment)
}

val nullObject = primitiveObject(null)