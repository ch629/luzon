package com.luzon.recursive_descent.expression

import com.luzon.lexer.Token
import com.luzon.recursive_descent.RecursiveDescentStream

class ExpressionRecursiveDescentStream(tokens: ExpressionStream) : RecursiveDescentStream<ExpressionToken>(tokens) {
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenType == tokenEnum }
}
