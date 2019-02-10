package com.luzon.runtime

data class LzObject(val type: String, val value: Any)

val nullObject = LzObject("NULL", -1)