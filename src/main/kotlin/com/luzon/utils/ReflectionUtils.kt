package com.luzon.utils

import com.luzon.fsm.FiniteStateMachine
import com.luzon.fsm.State
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

fun KClass<*>.constructorsToFSM(): FiniteStateMachine<KClass<*>, KFunction<KClass<*>>> {
    val root = State<KClass<*>, KFunction<KClass<*>>>()
    var pointer = root

    constructors.forEach { con ->
        con.parameters.forEach { param ->
            val newState = State<KClass<*>, KFunction<KClass<*>>>()

            pointer.addTransition({ it.isSubclassOf(param.type.classifier as KClass<*>) }, newState)
            pointer = newState
        }

        pointer.accepting = con as KFunction<KClass<*>>
        pointer = root
    }

    return FiniteStateMachine(listOf(root))
}

private val constructorFSMCache = hashMapOf<KClass<*>, FiniteStateMachine<KClass<*>, KFunction<KClass<*>>>>()
fun <T : Any> tryConstructorArguments(clazz: KClass<T>, vararg args: Any): T? {
    if (!constructorFSMCache.containsKey(clazz)) constructorFSMCache[clazz] = clazz.constructorsToFSM()
    val fsm = constructorFSMCache[clazz]!!.copyOriginal()

    args.forEach { fsm.accept(it::class) }

    return (fsm.acceptValue as? KFunction<T>?)?.call(*args)
}

inline fun <reified T : Any> tryConstructorArgs(vararg args: Any) = tryConstructorArguments(T::class, *args)
inline fun <reified T : Any> tryConstructorArgsList(list: List<Any>) = tryConstructorArgs<T>(*list.toTypedArray())

fun <T : Any> KClass<T>.getConstructorParameters() = constructors.map { con ->
    con.parameters.map { param ->
        param.type.classifier as KClass<*>
    }
}

inline fun <reified T : Any> getConstructorParameters() = T::class.getConstructorParameters()
