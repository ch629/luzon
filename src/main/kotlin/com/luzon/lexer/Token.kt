package com.luzon.lexer

data class Token(val tokenEnum: TokenEnum, val data: String) {
    override fun toString() = when (tokenEnum) {
        is Literal -> "($tokenEnum, $data)"
        else -> "$tokenEnum"
    }
}