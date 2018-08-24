package com.luzon.parser

//Used to easily distinguish between unary and binary minus for the ShuntingYard
enum class ExpressionOperators {
    PLUS, MINUS, MULTIPLY, DIVIDE, UNARY_MINUS
}