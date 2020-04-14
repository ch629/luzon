package lexer

import com.luzon.fsm.FiniteStateMachine
import com.luzon.lexer.Token
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.TokenMachine
import com.luzon.lexer.Tokenizer
import org.amshove.kluent.shouldBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.Paths

object TokenTest : Spek({
    describe("a token file") {
        val fsm = TokenMachine.getFSM()

        it("should successfully convert to an FSM") {
            fsm.running shouldBe true
        }

        it("should successfully recognize 'for' as a Keyword.FOR, and additions to that as a Literal.IDENTIFIER") {
            fsm.accept("for")
            fsm.accepting shouldBe true
            fsm.acceptValue shouldBe Token.Keyword.FOR
        }

        it("should recognize additions to 'for' as a Literal.IDENTIFIER") {
            fsm.accept('T')
            fsm.accepting shouldBe true
            fsm.acceptValue shouldBe Literal.IDENTIFIER
        }

        it("should successfully tokenize the same code multiple times") {
            val testLz =
                Files.readAllLines(Paths.get(TokenTest::class.java.getResource("Test.lz").toURI()))
                    .joinToString("\n")

            val firstCount = Tokenizer(testLz).findTokens().count() // 25

            (0..10).forEach { _ ->
                val stream = Tokenizer(testLz).findTokens()
                val c = stream.count()

                c shouldBe firstCount
            }
        }

        it("should tokenize a string successfully") {
            val fsm = TokenMachine.getFSM()
            val string = "\"hello\""

            fsm.accept(string)

            fsm.acceptValue shouldBe Literal.STRING
        }

        it("should tokenize multiple items together") {
            val tokens = Tokenizer("val c = \"hello\"").findTokens().toList()

            tokens[0].tokenEnum shouldBe Keyword.VAL
            tokens[1].tokenEnum shouldBe Literal.IDENTIFIER
            tokens[2].tokenEnum shouldBe Token.Symbol.EQUAL
            tokens[3].tokenEnum shouldBe Literal.STRING
        }
    }
})

private fun FiniteStateMachine<Char, Token.TokenEnum>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}
