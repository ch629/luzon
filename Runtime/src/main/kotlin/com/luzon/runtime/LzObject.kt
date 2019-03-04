package com.luzon.runtime

fun primitiveObject(value: Any?) = LzObject(clazz = LzClass("PRIMITIVE"), value = value)

data class LzObject(val clazz: LzClass, val value: Any? = null, val environment: Environment = Environment.global) {
    operator fun get(name: String) = environment[name]
}

val nullObject = primitiveObject(null) // TODO: Or Unit?