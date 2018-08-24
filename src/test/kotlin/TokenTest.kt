import com.luzon.fsm.FSMachine
import com.luzon.kodein
import com.luzon.lexer.Keyword
import com.luzon.lexer.Literal
import com.luzon.lexer.TokenEnum
import com.luzon.lexer.TokenRegexJson
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeGreaterThan
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.kodein.di.generic.instance

object TokenTest : Spek({
    given("a token file") {
        val tokens: TokenRegexJson by kodein.instance()
        it("should load correctly into a TokenRegexJson") {
            tokens.keywords.size shouldBeGreaterThan 0
        }

        val fsm = tokens.toFSM()
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
