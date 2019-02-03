package com.luzon.rd.ast

sealed class BooleanExpression {
    sealed class Binary(val left: BooleanExpression, val right: BooleanExpression) : BooleanExpression() {
        class Equals(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class NotEquals(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class GreaterEquals(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class Greater(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class Less(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class LessEquals(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)

        class And(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
        class Or(left: BooleanExpression, right: BooleanExpression) : Binary(left, right)
    }

    sealed class Unary(val value: BooleanExpression) {
        class Not(value: BooleanExpression) : Unary(value)
    }

    sealed class Literal(val value: Boolean) : BooleanExpression() {
        object True : Literal(true)
        object False : Literal(false)
    }

    data class ExpressionBoolean(val expression: Expression) : BooleanExpression()
}