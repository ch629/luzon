package com.luzon.runtime

object ClassReferenceTable {
    val classMap = hashMapOf<String, LzClass>()

    operator fun get(name: String) = classMap[name]
    operator fun plusAssign(clazz: LzClass) {
        classMap += clazz.name to clazz
    }

    fun reset() {
        classMap.clear()
    }
}