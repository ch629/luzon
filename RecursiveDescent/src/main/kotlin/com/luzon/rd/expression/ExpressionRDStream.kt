package com.luzon.rd.expression

import com.luzon.lexer.Token
import com.luzon.rd.RDStream

class ExpressionRDStream(tokens: ExpressionStream) : RDStream<ExpressionToken>(tokens) {
    fun matchConsume(tokenEnum: Token.TokenEnum) = matchConsume { it.tokenType == tokenEnum }
}