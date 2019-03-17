package com.luzon.runtime

import com.luzon.recursive_descent.ast.ASTNode
import com.luzon.recursive_descent.expression.accept
import com.luzon.runtime.visitors.RuntimeVisitor

interface Invokable {
    operator fun invoke(environment: Environment, args: List<LzObject>): LzObject
}

// TODO: This probably shouldn't use the FunctionParameter type, as I won't be able to check subtypes, and it only stores the type as a String which is not ideal
open class LzFunction(val name: String, val params: List<ASTNode.FunctionParameter>, val returnType: LzType<*>?,
                      val block: ASTNode.Block = ASTNode.Block(emptyList())) : Invokable {

    override fun invoke(environment: Environment, args: List<LzObject>): LzObject {
        var returnObject: LzObject? = null

        // Load arguments into the environment
        withEnvironment(environment.newEnv()) {
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

    fun argumentsMatchParams(args: List<LzObject>): Boolean {
        if (args.size == params.size) {
            args.forEachIndexed { index, arg ->
                if (params[index].type != "Any" && arg.clazz.name != params[index].type)
                    return false
            }

            return true
        }

        return false
    }
}

class LzCodeFunction(name: String, params: List<ASTNode.FunctionParameter>, returnType: LzType<*>?, var function: (Environment, List<LzObject>) -> LzObject = { _, _ -> nullObject }) :
        LzFunction(name, params, returnType, ASTNode.Block(emptyList())) {
    override fun invoke(environment: Environment, args: List<LzObject>): LzObject = function(environment, args)
}