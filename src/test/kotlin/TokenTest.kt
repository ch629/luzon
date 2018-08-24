
import com.luzon.fsm.FSMachine
import com.luzon.kodein
import com.luzon.lexer.Keyword
import com.luzon.lexer.Literal
import com.luzon.lexer.TokenEnum
import com.luzon.lexer.TokenMachine
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.kodein.di.generic.instance

object TokenTest : Spek({
    given("a token file") {
        val tokens: TokenMachine by kodein.instance()
        val fsm = tokens.getFSM()
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

private fun FSMachine<Char, TokenEnum>.getOutput() = getCurrentOutput().first()
