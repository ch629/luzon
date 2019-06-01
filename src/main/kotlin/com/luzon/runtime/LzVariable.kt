package com.luzon.runtime

class LzVariable(name: String, value: LzObject, type: LzClass, private val const: Boolean) : LzTypedValue(name, value, type) {
    fun assign(newValue: LzObject): Boolean {
        if (const) return false // TODO: Throw reassigning const exception
        if (isNotAssignableFrom(newValue)) return false // TODO: Throw assigned type exception

        value = newValue
        return true
    }
}

open class LzTypedValue(val name: String, var value: LzObject, var type: LzClass) {
    fun isAssignableFrom(obj: LzObject) = obj.clazz.isSubclassOf(type)
    fun isNotAssignableFrom(obj: LzObject) = !isAssignableFrom(obj)
}