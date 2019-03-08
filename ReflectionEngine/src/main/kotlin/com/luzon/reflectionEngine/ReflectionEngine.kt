package com.luzon.reflectionEngine

import com.luzon.rd.ast.ASTNode
import com.luzon.reflectionEngine.annotations.LzMethod
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

                if (annotation != null) {
                    val params = annotation.args
                            .mapIndexed { index, s -> ASTNode.FunctionParameter(index.toString(), s) }

                    Environment.global.defineFunction(
                            LzCodeFunction(if (annotation.name.isEmpty()) func.name else annotation.name,
                                    params, null) { env, args ->
                                func.call(instancedClass, env, args) as LzObject
                            })
                }
            }
        }
    }
}

class TestMethods {
    @LzMethod(args = ["Int"])
    fun println(env: Environment, args: List<LzObject>): LzObject {
        println("CALLED ${args[0]}")
        return nullObject
    }
}

fun main() {
    ReflectionEngine.registerClassMethods(TestMethods::class)

    Environment.global.invokeFunction("println", listOf(primitiveObject(5)))
}