package com.luzon.reflection_engine.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LzMethod(val name: String = "", vararg val args: String = [])
