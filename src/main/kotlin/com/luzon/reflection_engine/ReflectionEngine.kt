package com.luzon.reflection_engine

import com.luzon.recursive_descent.ast.SyntaxTreeNode
import com.luzon.reflection_engine.annotations.LzMethod
import com.luzon.runtime.Environment
import com.luzon.runtime.LzCodeFunction
import com.luzon.runtime.LzObject
import com.luzon.runtime.nullObject
import com.luzon.runtime.primitiveObject
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaType

object ReflectionEngine {
    @Deprecated("Replaced with a system that doesn't require lists as arguments")
    fun registerClassMethods(clazz: KClass<*>) {
        val instancedClass = clazz.objectInstance ?: clazz.createInstance()
        clazz.functions.forEach { func ->
            if (func.returnType.classifier == LzObject::class) {
                val annotation = func.findAnnotation<LzMethod>() ?: return

                if (func.parameters.size == 2) {
                    // Check if the args list is of LzObject or Any
                    val lzObjects = func.parameters[1].type.arguments[0].type?.classifier == LzObject::class

                    val params = annotation.args
                        .mapIndexed { index, s -> SyntaxTreeNode.FunctionParameter(index.toString(), s) }

                    Environment.global.defineFunction(
                        LzCodeFunction(if (annotation.name.isEmpty()) func.name else annotation.name,
                            params, null) { _, args ->
                            func.call(instancedClass, if (lzObjects) args else args.map { it.value }) as LzObject
                        })
                }
            }
        }
    }

    fun registerNewClassMethods(clazz: KClass<*>) {
        val instancedClass = clazz.objectInstance ?: clazz.createInstance()
        clazz.functions.forEach { func ->
            val annotation = func.findAnnotation<LzMethod>() ?: return

            val funName = if (annotation.name.isEmpty()) func.name else annotation.name
            val params = func.parameters.map {
                // TODO: Temporary way to deal with this, need to use some system for typing in Luzon so I'm not using Strings throughout to refer to everything.
                var type = it.type.javaType.typeName
                if (type == "java.lang.Object") type = "Any"
                SyntaxTreeNode.FunctionParameter(it.name ?: it.index.toString(), type.capitalize())
            }

            var retType: String? = func.returnType.javaType.typeName
            retType = when (retType) {
                "java.lang.Object" -> "Any"
                "void" -> null
                else -> retType!!.capitalize()
            }

            Environment.global.defineFunction(LzCodeFunction(funName, params.drop(1), retType) { _, args ->
                // TODO: Need a system to convert kotlin/java objects into a Luzon one without it being a primitive.
                val valueArgs = args.map { it.value }.toMutableList()
                valueArgs.add(0, instancedClass)
                val call = func.call(*valueArgs.toTypedArray())
                if (call != null) primitiveObject(call)
                nullObject
            })
        }
    }
}
