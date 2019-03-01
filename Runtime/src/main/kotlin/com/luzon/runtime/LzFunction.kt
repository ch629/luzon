package com.luzon.runtime

import com.luzon.rd.ast.ASTNode

interface Invokable {
    fun invoke(environment: Environment, args: List<LzObject>): LzObject
}

data class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?,
                      val block: ASTNode.Block?) : Invokable {
    override fun invoke(environment: Environment, args: List<LzObject>): LzObject {
        // TODO: Check args match the params.

        args.forEachIndexed { i, obj ->
            environment += params[i].name to obj
        }

        return nullObject
    }
}