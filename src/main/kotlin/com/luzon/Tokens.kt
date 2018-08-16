package com.luzon

import com.luzon.Operators.*

enum class Operators(private val id: String? = null) {
    PLUS("plus"),
    MINUS("subtract"),
    MULTIPLY("multiply"),
    DIVIDE("divide"),
    EQUALS("equal"),
    EQUALITY("equalEqual"),
    NOT("not"),
    NOT_EQUALS("notEqual"),
    GREATER_THAN("greater"),
    LESS_THAN("less"),
    GREATER_THAN_EQUALS("greaterEqual"),
    LESS_THAN_EQUALS("lessEqual"),
    AND("and"),
    OR("or"),
    MOD("mod"),
    PLUS_ASSIGN("plusAssign"),
    MINUS_ASSIGN("subtractAssign"),
    MULTIPLY_ASSIGN("multiplyAssign"),
    DIVIDE_ASSIGN("divideAssign"),
    MOD_ASSIGN("modAssign"),
    INCREMENT("increment"),
    DECREMENT("decrement");

    fun getName() = id ?: name
}

private val tokens = hashMapOf<String, Token>()

//TODO: Maybe put all these within a class or something and make them objects.
sealed class Token(val PRIORITY: Int = 0, val id: String) {
    init {
        tokens[id] = this
    }
}

data class ID(val name: String, val value: Token) : Token(-100, "literal:identifier") //TODO: Point to a location in the symbol table?

data class OPERATOR(val op: Operators) : Token(id = op.getName())

sealed class LiteralToken<T>(val value: T, name: String) : Token(id = "literal:$name")

//TODO: Might not be able to use Kotlin Types here unless I do checks on all numbers for type checking.

class INTEGER_LITERAL(i: Int) : LiteralToken<Int>(i, "int")
class FLOAT_LITERAL(f: Float) : LiteralToken<Float>(f, "float")
class DOUBLE_LITERAL(d: Double) : LiteralToken<Double>(d, "double")
class STRING_LITERAL(s: String) : LiteralToken<String>(s, "string")
class CHAR_LITERAL(c: Char) : LiteralToken<Char>(c, "char")
class BOOLEAN_LITERAL(b: Boolean) : LiteralToken<Boolean>(b, "boolean")

//TODO: These should probably be used after the lexer stage, when making an AST.
//TODO: Interpreter Pattern?
sealed class Expression<T>(val PRIORITY: Int = 0) { //TODO: Maybe make a class to hold T for Expressions so I can easily implement things like plus i.e. a.plus(b) without worrying about types outside
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
            MOD_ASSIGN -> TODO()
            INCREMENT -> TODO()
            DECREMENT -> TODO()
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
            MOD_ASSIGN -> TODO()
            INCREMENT -> TODO()
            DECREMENT -> TODO()
            else -> TODO()
        }
    }
}