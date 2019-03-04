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
            val paramNames = it.params.joinToString(",") { it.type }
            functionsMap += "${it.name}($paramNames)" to it
        }
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

    private fun List<LzObject>.toFunctionParams(): String = joinToString(",") { it.clazz.name }

    fun invokeFunction(name: String, args: List<LzObject>, environment: Environment) =
            functionsMap["$name(${args.toFunctionParams()})"]?.invoke(environment, args)
}