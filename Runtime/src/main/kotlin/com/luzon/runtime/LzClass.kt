package com.luzon.runtime

import com.luzon.rd.ast.ASTNode

data class LzClass(val name: String, val constructor: LzFunction = LzFunction(name, emptyList(), null, null), val functions: List<LzFunction>, val parentEnvironment: Environment = EnvironmentManager.currentEnvironment, val block: ASTNode.Block) {
    fun newInstance(args: List<LzObject>): LzInstance? {
        val environment = parentEnvironment.newEnv()

        return if (constructor.params.size == args.size) {
            constructor.invoke(environment, args)

            LzInstance(environment)
        } else null
    }
}

data class LzInstance(val environment: Environment)