package com.luzon.recursive_descent.ast

import com.luzon.utils.indent
import kotlin.reflect.KClass

fun main() {
    VisitorGenerator.generate("")
}

object VisitorGenerator {
    fun generate(path: String) {
        val lines = findNodes()
        val visitorSb = StringBuilder()
        val acceptSb = StringBuilder()
        visitorSb.appendln("package com.luzon.recursive_descent.expression\n")
        visitorSb.appendln("import com.luzon.recursive_descent.ast.ASTNode\n")
        visitorSb.appendln("interface ASTNodeVisitor<T> {")
        acceptSb.appendln("fun <T> ASTNode.accept(visitor: ASTNodeVisitor<T>) = when (this) {")

        lines.forEach {
            val line = it.trim()

            visitorSb.indent().appendln("fun visit(node: $line): T = throw NotImplementedError(\"$line visitor not implemented.\")")
            acceptSb.indent().appendln("is $line -> visitor.visit(this)")
        }

        acceptSb.indent().appendln("else -> throw NotImplementedError(\"Hit else when accepting an ASTNode.\")")

        visitorSb.appendln("}")
        acceptSb.appendln("}")

//        val path = Paths.get("$path\\com\\luzon\\recursive_descent\\expression\\ASTNodeVisitor.kt")
//        Files.deleteIfExists(path)
//        Files.createDirectories(path.parent)
//        Files.createFile(path)

//        Files.write(path, visitorSb.appendln().appendln(acceptSb).toString().toByteArray())

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