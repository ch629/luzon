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
                    val tokenizer = Tokenizer(code)
                    tokenizer.tokensAsString() shouldEqual output.map {
                        when (it) {
                            is Keyword -> KeywordContainer(it)
                            is Symbol -> SymbolContainer(it)
                            is Literal -> LiteralContainer(it)
                            is Comment -> CommentContainer(it)
                            else -> None
                        }
                    }.joinToString(" ") { it.toString() }
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
                arrayOf(
                        Keyword.FOR,
                        Symbol.L_PAREN,
                        Keyword.VAR,
                        Literal.IDENTIFIER,
                        Keyword.IN,
                        Literal.INT,
                        Symbol.RANGE,
                        Literal.INT,
                        Symbol.R_PAREN,
                        Symbol.L_BRACE,
                        Keyword.IF,
                        Symbol.L_PAREN,
                        Literal.IDENTIFIER,
                        Symbol.EQUAL_EQUAL,
                        Literal.INT,
                        Symbol.R_PAREN,
                        Symbol.L_BRACE,
                        Keyword.VAL,
                        Literal.IDENTIFIER,
                        Symbol.EQUAL,
                        Literal.STRING,
                        Comment.COMMENT_MULTI,
                        Comment.COMMENT_SINGLE,
                        Symbol.R_BRACE,
                        Symbol.R_BRACE
                )

)