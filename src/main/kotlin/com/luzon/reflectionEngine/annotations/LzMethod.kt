package com.luzon.reflectionEngine.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class LzMethod(val name: String = "", vararg val args: String = [])