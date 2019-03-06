package com.luzon.runtime.visitors

import com.luzon.lexer.Tokenizer
import com.luzon.rd.RecursiveDescent
import com.luzon.rd.TokenRDStream
import com.luzon.rd.ast.ASTNode
import com.luzon.rd.expression.ASTNodeVisitor
import com.luzon.rd.expression.accept
import com.luzon.runtime.*

fun main() {
    val time = System.currentTimeMillis()
    val code = """
        class Test {
            var testName: Int = add(5, 2)

            fun add(a: Int, b: Int): Int {
                return a + b
            }

            fun test(): Int {
                // testName = add(8, 3)

                return Test().testName
            }
        }
    """.trimIndent()
//    val tokens = Tokenizer(code).tokensAsString()

    val tree = RecursiveDescent(TokenRDStream(Tokenizer(code).findTokens())).parse()
    tree?.accept(ClassVisitor)

    val a = ClassReferenceTable.classMap

    val d = ClassReferenceTable["Test"]?.newInstance(emptyList())
    val e = d?.invokeFunction("add", listOf(primitiveObject(5), primitiveObject(6)))
    val f = d?.invokeFunction("test", listOf())

    println("Finished in ${System.currentTimeMillis() - time}ms")
}

object ClassVisitor : ASTNodeVisitor<Any> {
    override fun visit(node: ASTNode.Class) {
        val (name, constructor, block) = node

        val constructorFunction =
                if (constructor != null) visit(constructor)
                else LzFunction(name, emptyList(), null)

        ClassReferenceTable += LzClass(name, constructorFunction, processClassFunctions(block),
                EnvironmentManager.currentEnvironment, block)
    }

    override fun visit(node: ASTNode.ConstructorVariableDeclaration) =
            ASTNode.FunctionParameter(node.name, node.type)

    override fun visit(node: ASTNode.Constructor) =
            LzFunction("", node.variables.map { visit(it) }, null)

    // TODO: Return Type? Maybe make it a String rather than LzType?
    override fun visit(node: ASTNode.FunctionDefinition) =
            LzFunction(node.name, node.parameters, null, node.block)

    // TODO: Secondary Constructors
    private fun processClassFunctions(node: ASTNode.Block) = node.nodes.filter { it is ASTNode.FunctionDefinition }.map {
        visit(it as ASTNode.FunctionDefinition)
    }
}