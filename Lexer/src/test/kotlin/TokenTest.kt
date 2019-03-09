import com.luzon.fsm.FSM
import com.luzon.lexer.Token
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.TokenMachine
import com.luzon.lexer.Tokenizer
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Files
import java.nio.file.Paths

object TokenTest : Spek({
    given("a token file") {
        var fsm = TokenMachine.getFSM()
        // TODO: Not sure why this happens, but for the gradle build, I need to copy it again for it to work correctly.
        fsm = fsm.copyOriginal()

        it("should successfully convert to an FSM") {
            fsm.running shouldBe true
        }

        it("should successfully recognize 'for' as a Keyword.FOR, and additions to that as a Literal.IDENTIFIER") {
            fsm.accept("for")
            fsm.accepting shouldBe true
            fsm.acceptValue shouldBe Keyword.FOR
        }

        it("should recognize additions to 'for' as a Literal.IDENTIFIER") {
            fsm.accept('T')
            fsm.accepting shouldBe true
            fsm.acceptValue shouldBe Literal.IDENTIFIER
        }
    }

    it("should successfully tokenize the same code multiple times") {
        val testLz =
                Files.readAllLines(Paths.get(TokenTest::class.java.getResource("Test.lz").toURI()))
                        .joinToString("\n")

        val firstCount = Tokenizer(testLz).findTokens().count() // 25

        (0..10).forEach {
            val stream = Tokenizer(testLz).findTokens()
            val c = stream.count()

            c shouldBe firstCount

        }
    }
})

private fun FSM<Char, Token.TokenEnum>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}