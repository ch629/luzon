package com.luzon

import com.luzon.lexer.TokenMachine
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
        ReflectionEngine.registerNewClassMethods(clazz)
    }

    /**
     * Clears memory used by the lexer, stored within the Token matching FSM, this will be regenerated
     * which will take some time on the next lexer usage.
     */
    fun clearLexerMemory() {
        TokenMachine.clearFSM()
    }
}
