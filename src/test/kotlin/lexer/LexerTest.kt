package lexer

import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.Token.Symbol
import com.luzon.lexer.Token.TokenEnum
import com.luzon.lexer.Tokenizer
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LexerTest : Spek({
    describe("a lexer") {
        context("examples") {
            examples.forEach { (code, output) ->
                it("should match the expected output") {
                    val tokenizerString = Tokenizer(code).tokensAsString()
                    val outputString = output.joinToString(" ") {
                        if (it is Pair<*, *>) "${it.first}(${it.second})"
                        else "${it as TokenEnum}"
                    }

                    tokenizerString shouldEqual outputString
                }
            }
        }
    }
})

private val examples = arrayOf(
    """
            for(var i in 1..5) {
                if(i == 2) {
                    val c = "test"
                    /*
                        Multi Line Comment
                    */

                    //Single Line Comment
                }
            }
        """.trimIndent()
        to
        arrayOf<Any>(
            Keyword.FOR,
            Symbol.L_PAREN,
            Keyword.VAR,
            Literal.IDENTIFIER to "i",
            Keyword.IN,
            Literal.INT to 1,
            Symbol.RANGE,
            Literal.INT to 5,
            Symbol.R_PAREN,
            Symbol.L_BRACE,
            Keyword.IF,
            Symbol.L_PAREN,
            Literal.IDENTIFIER to "i",
            Symbol.EQUAL_EQUAL,
            Literal.INT to 2,
            Symbol.R_PAREN,
            Symbol.L_BRACE,
            Keyword.VAL,
            Literal.IDENTIFIER to "c",
            Symbol.EQUAL,
            Literal.STRING to "test",
//                        Comment.COMMENT_MULTI,
//                        Comment.COMMENT_SINGLE,
            Symbol.R_BRACE,
            Symbol.R_BRACE
        )
)
