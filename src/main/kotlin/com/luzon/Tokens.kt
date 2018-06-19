package com.luzon

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

sealed class Token

data class ID(val name: String, val value: Token) : Token() //TODO: Point to a location in the symbol table?

data class OPERATOR(val op: Operators) : Token()

sealed class LiteralToken<T>(val t: T) : Token()

class INTEGER_LITERAL(i: Int) : LiteralToken<Int>(i)
class FLOAT_LITERAL(f: Float) : LiteralToken<Float>(f)
class DOUBLE_LITERAL(d: Double) : LiteralToken<Double>(d)
class STRING_LITERAL(s: String) : LiteralToken<String>(s)
class CHAR_LITERAL(c: Char) : LiteralToken<Char>(c)
class BOOLEAN_LITERAL(b: Boolean) : LiteralToken<Boolean>(b)