package com.luzon.lexer

import com.luzon.fsm.IFsm
import com.luzon.fsm.scanner.StringScanner

data class Token(val tokenEnum: TokenEnum, val data: String) {
    override fun toString() = when (tokenEnum) {
        is Literal -> "$tokenEnum($data)"
        else -> "$tokenEnum"
    }

    interface TokenEnum {
        fun id() = when (this) {
            is Enum<*> -> generateID()
            else -> null
        }

        fun regex() = when (this) {
            is Literal -> regex
            is Comment -> regex
            is Keyword -> if (capitalize) generateID().capitalize() else generateID()
            is Symbol -> regex.replaceMetaCharacters()
            else -> ""
        }

        fun isId(id: String) = id() == id

        fun toToken(data: String) = Token(this, data)
        fun toToken() = toToken("")

        fun toFSM() = IFsm.fromRegex<TokenEnum>(regex()).replaceChildOutputs(this)
    }

    object None : TokenEnum {
        override fun toString() = "none"
    }

    enum class Keyword(val capitalize: Boolean = false) : TokenEnum {
        FOR, WHILE, IF, ELSE, WHEN, BREAK,
        VAR, VAL, FUN, CLASS, ABSTRACT, ENUM,
        DOUBLE(true),
        FLOAT(true),
        INT(true),
        STRING(true),
        CHAR(true),
        BOOLEAN(true),
        IS, AS, IN, PRIVATE;

        override fun toString() = "keyword:${id()}"
    }

    enum class Symbol(val regex: String) : TokenEnum {
        EQUAL("="), EQUAL_EQUAL("=="), NOT("!"),
        NOT_EQUAL("!="), GREATER(">"), LESS("<"),
        GREATER_EQUAL(">="), LESS_EQUAL("<="),
        AND("&&"), OR("||"), PLUS("+"),
        SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/"),
        MOD("%"), PLUS_ASSIGN("+="), SUBTRACT_ASSIGN("-="),
        MULTIPLY_ASSIGN("*="), DIVIDE_ASSIGN("/="),
        MOD_ASSIGN("%="), TYPE(":"), INCREMENT("++"),
        DECREMENT("--"), L_PAREN("("), R_PAREN(")"),
        L_BRACE("{"), R_BRACE("}"), L_BRACKET("["),
        R_BRACKET("]"), RANGE(".."), ARROW("->"),
        DOT("."), COMMA(",");

        override fun toString() = "symbol:${id()}"
    }

    enum class Literal(val regex: String) : TokenEnum {
        DOUBLE("\\d+d|\\d+\\.\\d+d?"),
        FLOAT("\\d+f|\\d+\\.\\d+f"),
        INT("\\d+"),
        STRING("\".*\""),
        CHAR("'\\?.'"),
        BOOLEAN("true|false"),
        IDENTIFIER("[A-Za-z_]\\w*");

        override fun toString() = "literal:${id()}"
    }

    enum class Comment(val regex: String) : TokenEnum {
        COMMENT_SINGLE("//.*\n"),
        COMMENT_MULTI("/\\*[.\n]*\\*/");

        override fun toString() = "comment:${id()}"
    }

    companion object {
        private fun String.replaceMetaCharacters() = replaceMetacharacters(this)

        private fun replaceMetacharacters(regex: String): String {
            var newRegex = regex

            "\\*+?[]().".forEach {
                newRegex = newRegex.replace("$it", "\\$it")
            }

            return newRegex
        }

        private fun Enum<*>.generateID(): String {
            val sb = StringBuffer()
            val scanner = StringScanner(name.toLowerCase())

            while (!scanner.isAtEnd()) {
                var c = scanner.advance()
                if (c == '_') c = scanner.advance().toUpperCase()

                sb.append(c)
            }

            return sb.toString()
        }
    }
}