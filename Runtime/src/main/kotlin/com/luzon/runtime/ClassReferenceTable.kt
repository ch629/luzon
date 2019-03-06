package com.luzon.runtime

object ClassReferenceTable {
    val classMap = hashMapOf<String, LzClass>()

    operator fun get(name: String) = classMap[name]
}