package com.luzon.runtime

import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

interface Invokable {
    operator fun invoke(environment: Environment, args: List<LzObject>): LzObject
}

// TODO: This probably shouldn't use the FunctionParameter type, as I won't be able to check subtypes, and it only stores the type as a String which is not ideal
data class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?,
                      val block: ASTNode.Block = ASTNode.Block(emptyList())) : Invokable {
    override fun invoke(environment: Environment, args: List<LzObject>): LzObject {
        // TODO: Check args match the params.
        var returnObject: LzObject? = null

        // Load arguments into the environment
        with(environment.newEnv()) {
            args.forEachIndexed { i, obj ->
                EnvironmentManager += params[i].name to obj
            }

            try {
                block.accept(RuntimeVisitor)
            } catch (ret: Return) {
                returnObject = ret.data
            }
        }

        return returnObject ?: nullObject
    }
}