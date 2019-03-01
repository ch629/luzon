package com.luzon.runtime

import com.luzon.rd.ast.ASTNode

data class LzClass(val name: String, val constructor: LzFunction, val functions: List<LzFunction>, val parentEnvironment: Environment, val block: ASTNode.Block) {
    fun newInstance(args: List<LzObject>): LzInstance? {
        if (constructor.params.size == args.size) {
            val environment = parentEnvironment.newEnv()
            val obj = constructor.invoke(environment, args)

            return LzInstance(environment)
        }

        return null
    }
}

data class LzInstance(val environment: Environment)