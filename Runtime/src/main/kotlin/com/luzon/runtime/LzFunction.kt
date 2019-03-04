package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

interface Invokable {
    fun invoke(environment: Environment, args: List<LzObject>): LzObject
}

// TODO: This probably shouldn't use the FunctionParameter type, as I won't be able to check subtypes, and it only stores the type as a String which is not ideal
data class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?,
                      val block: ASTNode.Block = ASTNode.Block(emptyList())) : Invokable {
    override fun invoke(environment: Environment, args: List<LzObject>): LzObject {
        // TODO: Check args match the params.

        // Load arguments into the environment
        val innerEnvironment = environment.newEnv()
        args.forEachIndexed { i, obj ->
            innerEnvironment += params[i].name to obj
        }

        // TODO: Or is this a good implementation, rather than using the environment as an argument for the visitors?
        with(innerEnvironment) {
            block.accept(RuntimeVisitor)
        }

        // TODO: Return statement, so I can return something other than a nullObject here.
        return nullObject
    }
}