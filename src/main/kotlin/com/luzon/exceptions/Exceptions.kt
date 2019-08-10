package com.luzon.exceptions

import com.luzon.lexer.Token
import java.lang.Exception

class InvalidTokenException(token: String, charNum: Int) : Exception("Invalid token: $token starting at character $charNum")

class UnexpectedTokenException(received: Token?, vararg tokens: Token.TokenEnum) : Exception("Expected: ${tokens.joinToString { it.toString() }} got: ${received
    ?: "none"}")

class TokenRuleException(string: String) : Exception("Expected rule of: $string but didn't receive a valid token.")
