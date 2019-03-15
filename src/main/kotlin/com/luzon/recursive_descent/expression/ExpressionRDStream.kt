package com.luzon.recursive_descent.expression

import com.luzon.lexer.Token
import com.luzon.recursive_descent.RDStream

class ExpressionRDStream(tokens: ExpressionStream) : RDStream<ExpressionToken>(tokens) {
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenType == tokenEnum }
}