import com.luzon.lexer.*
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object LexerTest : Spek({
    given("a lexer") {
        on("examples") {
            examples.forEach { (code, output) ->
                it("should match the expected output") {
                    val tokenizerString = Tokenizer(code).tokensAsString()
                    val outputString = output.joinToString(" ") {
                        if (it is Pair<*, *>) "(${it.first}, ${it.second})"
                        else "${it as TokenEnum}"
                    }

                    println("tokenizer: $tokenizerString")
                    println("output: $outputString")

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
                        Literal.INT to "1",
                        Symbol.RANGE,
                        Literal.INT to "5",
                        Symbol.R_PAREN,
                        Symbol.L_BRACE,
                        Keyword.IF,
                        Symbol.L_PAREN,
                        Literal.IDENTIFIER to "i",
                        Symbol.EQUAL_EQUAL,
                        Literal.INT to "2",
                        Symbol.R_PAREN,
                        Symbol.L_BRACE,
                        Keyword.VAL,
                        Literal.IDENTIFIER to "c",
                        Symbol.EQUAL,
                        Literal.STRING to "\"test\"",
                        Comment.COMMENT_MULTI,
                        Comment.COMMENT_SINGLE,
                        Symbol.R_BRACE,
                        Symbol.R_BRACE
                )
)