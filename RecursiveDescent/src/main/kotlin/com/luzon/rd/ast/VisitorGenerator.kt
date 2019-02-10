package com.luzon.rd.ast

import com.luzon.utils.indent
import kotlin.reflect.KClass

fun main() {
    VisitorGenerator.generate()
}

object VisitorGenerator {
    fun generate() {
        val lines = findNodes()
        val visitorSb = StringBuilder()
        val acceptSb = StringBuilder()
        visitorSb.appendln("interface ASTNodeVisitor<T> {")
        acceptSb.appendln("fun <T> ASTNode.accept(visitor: ASTNodeVisitor<T>) = when (this) {")

        lines.forEach {
            val line = it.trim()

            visitorSb.indent().appendln("fun visit(node: $line): T = throw NotImplementedError(\"$line visitor not implemented.\")")
            acceptSb.indent().appendln("is $line -> visitor.visit(this)")
        }

        visitorSb.appendln("}")
        acceptSb.appendln("}")

        println(visitorSb.toString())
        println(acceptSb.toString())
    }

    private fun findNodes(clazz: KClass<*> = ASTNode::class, prefix: String = "ASTNode"): List<String> = mutableListOf<String>().apply {
        clazz.sealedSubclasses.forEach {
            if (it.isSealed) addAll(findNodes(it, "$prefix.${it.simpleName!!}"))
            else add("$prefix.${it.simpleName!!}")
        }
    }
}