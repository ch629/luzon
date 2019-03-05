package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

open class LzClass(val name: String, val constructor: LzFunction = LzFunction(name, emptyList(), null),
                   functions: List<LzFunction> = emptyList(), val parentEnvironment: Environment = EnvironmentManager.currentEnvironment,
                   val block: ASTNode.Block = ASTNode.Block(emptyList())) {
    private val functionsMap = hashMapOf<String, LzFunction>()

    init {
        functions.forEach {
            functionsMap += it.params.toFunctionMapName(it.name) to it
        }

        // Register the constructor as a global function
        GlobalFunctionManager[constructor.params.toFunctionMapName(name)] = constructor
    }

    fun newInstance(args: List<LzObject>): LzObject? {
        val environment = parentEnvironment.copy()

        return if (constructor.params.size == args.size) {
            constructor.invoke(environment, args)

            // TODO: This, or use normal classes with Environment within the constructor?
            with(environment) {
                block.nodes.filter { it !is ASTNode.FunctionDefinition }.forEach {
                    it.accept(RuntimeVisitor)
                }
            }

            LzObject(this, null, environment)
        } else null
    }

    fun invokeFunction(name: String, args: List<LzObject>, environment: Environment) =
            functionsMap[getFunctionMapName(name, args)]?.invoke(environment, args)
}

internal fun List<LzObject>.toFunctionParams(): String = joinToString(",") { it.clazz.name }
internal fun getFunctionMapName(name: String, args: List<LzObject>) = "$name(${args.toFunctionParams()})"
internal fun List<ASTNode.FunctionParameter>.toFunctionMapName(name: String) =
        "$name(${joinToString(",") { it.type }})"