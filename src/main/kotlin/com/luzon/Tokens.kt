package com.luzon

import com.luzon.Operators.*

enum class Operators {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    EQUALS,
    EQUALITY,
    NOT,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_EQUALS,
    LESS_THAN_EQUALS,
    AND,
    OR,
    MOD,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN,
    NOT_ASSIGN,
    MOD_ASSIGN,
    INCREMENT,
    DECREMENT
}

sealed class Token(val PRIORITY: Int = 0)

data class ID(val name: String, val value: Token) : Token(-100) //TODO: Point to a location in the symbol table?

data class OPERATOR(val op: Operators) : Token()

sealed class LiteralToken<T>(val value: T) : Token()

//TODO: Might not be able to use Kotlin Types here unless I do checks on all numbers for type checking.

class INTEGER_LITERAL(i: Int) : LiteralToken<Int>(i)
class FLOAT_LITERAL(f: Float) : LiteralToken<Float>(f)
class DOUBLE_LITERAL(d: Double) : LiteralToken<Double>(d)
class STRING_LITERAL(s: String) : LiteralToken<String>(s)
class CHAR_LITERAL(c: Char) : LiteralToken<Char>(c)
class BOOLEAN_LITERAL(b: Boolean) : LiteralToken<Boolean>(b)

//TODO: Interpreter Pattern?
sealed class Expression<T>(PRIORITY: Int = 0) : Token(PRIORITY) { //TODO: Maybe make a class to hold T for Expressions so I can easily implement things like plus i.e. a.plus(b) without worrying about types outside
    abstract fun resolve(): T
}

class ExprID<T> : Expression<T>(-100) {
    override fun resolve(): T {
        TODO("Symbol Table")
    }
}

class ExprLiteral<T>(private val literal: LiteralToken<T>) : Expression<T>() {
    override fun resolve() = literal.value
}

class ExprParen<T>(private val expr: Expression<T>) : Expression<T>() {
    override fun resolve() = expr.resolve()

}

class LType<T>(val value: T) {
    fun <K, O> plus(other: LType<K>): LType<O> {
        TODO()
    }

    fun isNumerical() = value is Number
}

class ExprUnary<T>(private val op: Operators, private val expr: Expression<T>) : Expression<T>() {
    override fun resolve(): T {
        val exprVal = expr.resolve()

        return when (op) { //TODO: Remove non unary operators
            PLUS -> exprVal
            MINUS -> TODO()
            MULTIPLY -> TODO()
            DIVIDE -> TODO()
            EQUALS -> TODO()
            EQUALITY -> TODO()
            NOT -> TODO()
            NOT_EQUALS -> TODO()
            GREATER_THAN -> TODO()
            LESS_THAN -> TODO()
            GREATER_THAN_EQUALS -> TODO()
            LESS_THAN_EQUALS -> TODO()
            AND -> TODO()
            OR -> TODO()
            MOD -> TODO()
            PLUS_ASSIGN -> TODO()
            MINUS_ASSIGN -> TODO()
            MULTIPLY_ASSIGN -> TODO()
            DIVIDE_ASSIGN -> TODO()
            NOT_ASSIGN -> TODO()
            MOD_ASSIGN -> TODO()
            INCREMENT -> TODO()
            DECREMENT -> TODO()
            else -> TODO()
        }
    }
}

class ExprBinary<L, R>(private val op: Operators, val left: Expression<L>, val right: Expression<R>) : Expression<Any>() {
    override fun resolve(): Any {
        val leftVal = left.resolve()
        val rightVal = right.resolve()

        return when (op) {
            PLUS -> TODO()
            MINUS -> TODO()
            MULTIPLY -> TODO()
            DIVIDE -> TODO()
            EQUALS -> TODO()
            EQUALITY -> TODO()
            NOT -> TODO()
            NOT_EQUALS -> TODO()
            GREATER_THAN -> TODO()
            LESS_THAN -> TODO()
            GREATER_THAN_EQUALS -> TODO()
            LESS_THAN_EQUALS -> TODO()
            AND -> TODO()
            OR -> TODO()
            MOD -> TODO()
            PLUS_ASSIGN -> TODO()
            MINUS_ASSIGN -> TODO()
            MULTIPLY_ASSIGN -> TODO()
            DIVIDE_ASSIGN -> TODO()
            NOT_ASSIGN -> TODO()
            MOD_ASSIGN -> TODO()
            INCREMENT -> TODO()
            DECREMENT -> TODO()
            else -> TODO()
        }
    }
}