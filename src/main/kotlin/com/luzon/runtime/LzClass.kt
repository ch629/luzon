package com.luzon.runtime

import com.luzon.recursive_descent.ast.SyntaxTreeNode
import com.luzon.recursive_descent.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

open class LzClass(
    val name: String,
    val constructor: LzFunction = LzFunction(name, emptyList(), null),
    val functions: List<LzFunction> = emptyList(),
    val parentEnvironment: Environment = EnvironmentManager.currentEnvironment,
    val block: SyntaxTreeNode.Block = SyntaxTreeNode.Block(emptyList()),
    registerConstructor: Boolean = true
) {

    init {
        // Register the constructor as a global function
        if (registerConstructor) Environment.global
            .defineFunction(LzCodeFunction(constructor.name, constructor.params, constructor.returnType) { _, args ->
                newInstance(args) ?: nullObject
            })
    }

    fun newInstance(args: List<LzObject>): LzObject? {
        val environment = parentEnvironment.newEnv()

        return if (constructor.params.size == args.size) {
            constructor.invoke(environment, args)

            // TODO: This, or use normal classes with Environment within the constructor?
            withEnvironment(environment) {
                // Load functions into the environment
                functions.forEach {
                    environment += it
                }

                block.nodes.filter { it !is SyntaxTreeNode.FunctionDefinition }.forEach {
                    it.accept(RuntimeVisitor)
                }
            }

            LzObject(this, null, environment)
        } else null
    }

    fun isSubclassOf(other: LzClass): Boolean {
        return true // TODO: Complete this properly.
    }

    fun isNotSubclassOf(other: LzClass) = !isSubclassOf(other)
}
