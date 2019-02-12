package com.luzon.runtime

// TODO: Maybe make a class to hold the type, so we can implement primitives directly, then have one for CustomType where the implemented ones will be stored?
data class LzObject(val type: String, val value: Any)

val nullObject = LzObject("NULL", -1)