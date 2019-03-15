package com.luzon.runtime

import com.luzon.recursive_descent.ast.ASTNode
import com.luzon.recursive_descent.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

open class LzClass(val name: String, val constructor: LzFunction = LzFunction(name, emptyList(), null),
                   val functions: List<LzFunction> = emptyList(), val parentEnvironment: Environment = EnvironmentManager.currentEnvironment,
                   val block: ASTNode.Block = ASTNode.Block(emptyList()), registerConstructor: Boolean = true) {

    init {
        // Register the constructor as a global function
        if (registerConstructor) Environment.global
                .defineFunction(LzCodeFunction(constructor.name, constructor.params, constructor.returnType) { _, args ->
                    newInstance(args) ?: nullObject
                })

//        Environment.global.defineFunction(constructor)
    }

    fun newInstance(args: List<LzObject>): LzObject? {
//        val environment = parentEnvironment.copy()
        val environment = parentEnvironment.newEnv()

        return if (constructor.params.size == args.size) {
            constructor.invoke(environment, args)

            // TODO: This, or use normal classes with Environment within the constructor?
            withEnvironment(environment) {
                // Load functions into the environment
                functions.forEach {
                    environment += it
                }

                block.nodes.filter { it !is ASTNode.FunctionDefinition }.forEach {
                    it.accept(RuntimeVisitor)
                }
            }

            LzObject(this, null, environment)
        } else null
    }
}