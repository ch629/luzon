import com.luzon.fsm.FSM
import com.luzon.lexer.Token.*
import com.luzon.lexer.TokenMachine
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

object TokenTest : Spek({
    given("a token file") {
        val fsm = TokenMachine.getFSM()
        it("should successfully convert to an FSM") {
            fsm.isRunning() shouldBe true
        }

        it("should successfully recognize Float as a Keyword.FLOAT, and additions to that as a Literal.IDENTIFIER") {
            fsm.accept("Float")
            fsm.isAccepting() shouldBe true
            fsm.getOutput() shouldBe Keyword.FLOAT

            fsm.accept('T')
            fsm.isAccepting() shouldBe true
            fsm.getOutput() shouldBe Literal.IDENTIFIER
        }
    }
})

private fun FSM<Char, TokenEnum>.getOutput() = getCurrentOutput().first()
