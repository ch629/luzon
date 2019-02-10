package com.luzon.rd.ast

import com.luzon.utils.indent
import kotlin.reflect.KClass

object VisitorGenerator {
    fun generate() {
        val lines = findNodes()
        val visitorSb = StringBuilder()
        val acceptSb = StringBuilder()
        visitorSb.appendln("interface ASTNodeVisitor {")
        acceptSb.appendln("fun ASTNode.accept(visitor: ASTNodeVisitor) {")
        acceptSb.indent().appendln("when (this) {")

        lines.forEach {
            val trimmed = it.trim()

            visitorSb.indent().appendln("fun visit(node: $trimmed)")
            acceptSb.indent(2).appendln("is $trimmed -> visitor.visit(this)")
        }

        visitorSb.appendln("}")
        acceptSb.indent().appendln("}").appendln("}")

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