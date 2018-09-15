package com.luzon.utils

import com.luzon.fsm.FSM
import com.luzon.fsm.State
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

internal fun KClass<*>.constructorsToFSM(): FSM<KClass<*>, KFunction<KClass<*>>> {
    val root = State<KClass<*>, KFunction<KClass<*>>>()
    var pointer = root

    constructors.forEach { con ->
        con.parameters.forEach { param ->
            val newState = State<KClass<*>, KFunction<KClass<*>>>()

            pointer.addTransition({ it.isSubclassOf(param.type.classifier as KClass<*>) }, newState)
            pointer = newState
        }

        pointer.output = con as KFunction<KClass<*>>
        pointer = root
    }

    return FSM(root)
}

private val constructorFSMCache = hashMapOf<KClass<*>, FSM<KClass<*>, KFunction<KClass<*>>>>()
internal fun <T : Any> tryConstructorArguments(clazz: KClass<T>, vararg args: Any): T? { //TODO: Use Either here? -> Rather than nullable
    if (!constructorFSMCache.containsKey(clazz)) constructorFSMCache[clazz] = clazz.constructorsToFSM()
    val fsm = constructorFSMCache[clazz]!!.copy()

    args.forEach { fsm.accept(it::class) }

    return (fsm.getCurrentOutput().firstOrNull() as KFunction<T>?)?.call(*args)
}

internal inline fun <reified T : Any> tryConstructorArgs(vararg args: Any) = tryConstructorArguments(T::class, *args)
internal inline fun <reified T : Any> tryConstructorArgsList(list: List<Any>) = tryConstructorArgs<T>(*list.toTypedArray())

internal fun <T : Any> KClass<T>.getConstructorParameters() = constructors.map { con ->
    con.parameters.map { param ->
        param.type.classifier as KClass<*>
    }
}

internal inline fun <reified T : Any> getConstructorParameters() = T::class.getConstructorParameters()