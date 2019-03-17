package com.luzon.runtime

import kotlin.reflect.KClass

// TODO: Figure out if this is necessary, and how this would work with inheritance
sealed class LzType<T : Any>(val type: KClass<T>, val resolved: Boolean = true) {
    companion object {
        fun resolveType(name: String) = LzType::class.sealedSubclasses.firstOrNull {
            val objectInstance = it.objectInstance
            objectInstance != null && objectInstance.resolved && objectInstance.type.simpleName == name
        }?.objectInstance ?: LzCustomType(name)
    }
}

object LzBoolean : LzType<Boolean>(Boolean::class)

object LzInt : LzType<Int>(Int::class)
object LzFloat : LzType<Float>(Float::class)
object LzDouble : LzType<Double>(Double::class)
object LzString : LzType<String>(String::class)
data class LzCustomType(val name: String) : LzType<Unit>(Unit::class, false)
object LzNullType : LzType<Nothing>(Nothing::class, false)