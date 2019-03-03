package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

class LzClass(val name: String, val constructor: LzFunction = LzFunction(name, emptyList(), null, null),
              functions: List<LzFunction>, val parentEnvironment: Environment = EnvironmentManager.currentEnvironment,
              val block: ASTNode.Block) {
    private val functionsMap = hashMapOf<String, LzFunction>()

    init {
        functions.forEach {
            val paramNames = it.params.joinToString(",") { it.type }
            functionsMap += "${it.name}($paramNames)" to it
        }
    }

    fun newInstance(args: List<LzObject>): LzObject? {
        val environment = parentEnvironment.newEnv()

        return if (constructor.params.size == args.size) {
            constructor.invoke(environment, args)

            block.nodes.filter { it !is ASTNode.FunctionDefinition }.forEach {
                it.accept(RuntimeVisitor) // TODO: Environment?
            }

            // TODO: Should the value be Unit here?
            LzObject(LzCustomType(name), Unit, environment)
        } else null
    }

    private fun List<LzObject>.toFunctionParams(): String = joinToString(",") { it.type.type.simpleName!! }

    fun invokeFunction(name: String, args: List<LzObject>, environment: Environment): LzObject? {
        val potentialFunctions = functionsMap["$name(${args.toFunctionParams()})"]

        return potentialFunctions?.invoke(environment, args)
    }
}