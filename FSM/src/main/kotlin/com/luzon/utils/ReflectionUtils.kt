package com.luzon.utils

import com.luzon.fsm.OutputFSM
import com.luzon.fsm.OutputState
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

fun KClass<*>.constructorsToFSM(): OutputFSM<KClass<*>, KFunction<KClass<*>>> {
    val root = OutputState<KClass<*>, KFunction<KClass<*>>>()
    var pointer = root

    constructors.forEach { con ->
        con.parameters.forEach { param ->
            val newState = OutputState<KClass<*>, KFunction<KClass<*>>>()

            pointer.addTransition({ it.isSubclassOf(param.type.classifier as KClass<*>) }, newState)
            pointer = newState
        }

        pointer.output = con as KFunction<KClass<*>>
        pointer = root
    }

    return OutputFSM(root)
}

private val constructorFSMCache = hashMapOf<KClass<*>, OutputFSM<KClass<*>, KFunction<KClass<*>>>>()
fun <T : Any> tryConstructorArguments(clazz: KClass<T>, vararg args: Any): T? { //TODO: Use Either here? -> Rather than nullable
    if (!constructorFSMCache.containsKey(clazz)) constructorFSMCache[clazz] = clazz.constructorsToFSM()
    val fsm = constructorFSMCache[clazz]!!.copy()

    args.forEach { fsm.accept(it::class) }

    return (fsm.currentOutput.firstOrNull() as KFunction<T>?)?.call(*args)
}

inline fun <reified T : Any> tryConstructorArgs(vararg args: Any) = tryConstructorArguments(T::class, *args)
inline fun <reified T : Any> tryConstructorArgsList(list: List<Any>) = tryConstructorArgs<T>(*list.toTypedArray())

fun <T : Any> KClass<T>.getConstructorParameters() = constructors.map { con ->
    con.parameters.map { param ->
        param.type.classifier as KClass<*>
    }
}

inline fun <reified T : Any> getConstructorParameters() = T::class.getConstructorParameters()