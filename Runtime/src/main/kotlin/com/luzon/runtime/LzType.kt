package com.luzon.runtime

sealed class LzType<T>
object LzBoolean : LzType<Boolean>()
object LzInt : LzType<Int>()
object LzFloat : LzType<Float>()
object LzDouble : LzType<Double>()
object LzString : LzType<String>()
object LzFunctionType : LzType<LzFunction>()
object LzClassType : LzType<LzClass>()
object LzNullType : LzType<Nothing>()