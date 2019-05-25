package com.luzon

import com.luzon.lexer.Tokenizer
import com.luzon.recursive_descent.RecursiveDescent
import com.luzon.recursive_descent.TokenRDStream
import com.luzon.recursive_descent.expression.accept
import com.luzon.reflection_engine.ReflectionEngine
import com.luzon.runtime.ClassReferenceTable
import com.luzon.runtime.Environment
import com.luzon.runtime.visitors.ClassVisitor
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

object Luzon {
    fun runCode(code: String) {
        RecursiveDescent(TokenRDStream(Tokenizer(code).findTokens())).parse()?.accept(ClassVisitor)
    }

    fun runFile(path: String) {
        runCode(Files.readAllLines(Paths.get(path)).joinToString("\n"))
    }

    fun resetLanguage() {
        ClassReferenceTable.reset()
        Environment.global.reset()
    }

    fun registerMethods(clazz: KClass<*>) {
        ReflectionEngine.registerClassMethods(clazz)
    }
}

fun main() {
    val code = """
        class Test() {
            fun test(): Int {
                val s = "hello"
                val c = 'a'
                return 5
            }
        }
    """.trimIndent()

    val tokenizer = Tokenizer(code).tokensAsString()
    val tree = RecursiveDescent(TokenRDStream(Tokenizer(code).findTokens())).parse()

    Luzon.runCode(code)

    val f = "50f".toFloat()
    val d = "50d".toDouble()
}