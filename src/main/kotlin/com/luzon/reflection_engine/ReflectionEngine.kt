package com.luzon.reflection_engine

import com.luzon.recursive_descent.ast.ASTNode
import com.luzon.reflection_engine.annotations.LzMethod
import com.luzon.runtime.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

object ReflectionEngine {
    fun registerClassMethods(clazz: KClass<*>) {
        val instancedClass = clazz.objectInstance ?: clazz.createInstance()
        clazz.functions.forEach { func ->
            if (func.returnType.classifier == LzObject::class) {
                val annotation = func.findAnnotation<LzMethod>()

                if (annotation != null && func.parameters.size == 2) {
                    // Check if the args list is of LzObject or Any
                    val lzObjects = func.parameters[1].type.arguments[0].type?.classifier == LzObject::class

                    val params = annotation.args
                            .mapIndexed { index, s -> ASTNode.FunctionParameter(index.toString(), s) }

                    Environment.global.defineFunction(
                            LzCodeFunction(if (annotation.name.isEmpty()) func.name else annotation.name,
                                    params, null) { _, args ->
                                func.call(instancedClass, if (lzObjects) args else args.map { it.value }) as LzObject
                            })
                }
            }
        }
    }
}

class TestMethods {
    @LzMethod(args = ["Any"])
    fun println(args: List<Any>): LzObject {
        println("CALLED ${args[0]}")
        return nullObject
    }
}

fun main() {
    ReflectionEngine.registerClassMethods(TestMethods::class)

    Environment.global.invokeFunction("println", listOf(primitiveObject(5)))
}