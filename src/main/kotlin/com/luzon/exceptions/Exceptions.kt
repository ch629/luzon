package com.luzon.exceptions

import com.luzon.lexer.Token
import com.luzon.runtime.LzObject
import kotlin.reflect.KClass

class InvalidTokenException(token: String, charNum: Int) : Exception("Invalid token: $token starting at character $charNum.")

class UnexpectedTokenException(received: Token?, vararg tokens: Token.TokenEnum) : Exception("Expected: ${tokens.joinToString { it.toString() }} got: ${received
    ?: "none"}.")

class TokenRuleException(string: String) : Exception("Expected rule of: $string but didn't receive a valid token.")

class UnknownSymbolException(name: String) : Exception("Unknown symbol: $name.")

class UnknownFunctionException(name: String, args: List<LzObject>) : Exception("Unknown function: $name with args: $args.")

class SymbolExistsException(name: String) : Exception("A symbol already exists in this scope with name: $name.")

class InvalidTypeCastException(value: Any, type: KClass<*>) : Exception("Cannot convert value: $value to type: ${type.simpleName}.")

class InvalidWeightTypeException(value: Any) : Exception("Cannot weight value: $value for expressions.")

class InvalidPrecedenceTypeException(tokenType: Token.TokenEnum?) : Exception("Cannot get precedence for token of type: $tokenType.")
