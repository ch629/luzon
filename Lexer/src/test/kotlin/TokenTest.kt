
import com.luzon.fsm.IFsm
import com.luzon.lexer.Token.Keyword
import com.luzon.lexer.Token.Literal
import com.luzon.lexer.TokenMachine
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

object TokenTest : Spek({
    given("a token file") {
        var fsm = TokenMachine.getFSM()
        // TODO: Not sure why this happens, but for the gradle build, I need to copy it again for it to work correctly.
        fsm = fsm.copy()

        it("should successfully convert to an IFsm") {
            fsm.isRunning shouldBe true
        }

        it("should successfully recognize 'for' as a Keyword.FOR, and additions to that as a Literal.IDENTIFIER") {
            fsm.accept("for")
            fsm.isAccepting shouldBe true
            fsm.currentOutput.firstOrNull() shouldBe Keyword.FOR
        }

        it("should recognize additions to 'for' as a Literal.IDENTIFIER") {
            fsm.accept('T')
            fsm.isAccepting shouldBe true
            fsm.currentOutput.firstOrNull() shouldBe Literal.IDENTIFIER
        }
    }
})

private fun IFsm<Char>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}