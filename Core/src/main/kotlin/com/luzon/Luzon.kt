package com.luzon

import com.luzon.lexer.Tokenizer
import com.luzon.rd.RecursiveDescent
import com.luzon.rd.TokenRDStream
import com.luzon.rd.expression.accept
import com.luzon.reflectionEngine.ReflectionEngine
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