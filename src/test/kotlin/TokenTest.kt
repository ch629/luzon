import com.luzon.kodein
import com.luzon.lexer.*
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
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

            testOutput(fsm.getCurrentOutput().first().container, Keyword.FLOAT.toContainer())

            fsm.accept('T')
            fsm.isAccepting() shouldBe true

            val output = fsm.getCurrentOutput().first().container

            testOutput(output, Literal.IDENTIFIER.toContainer())
        }
    }
})

private fun testOutput(container: TokenContainer, shouldBe: TokenContainer) {
    when (shouldBe) {
        is LiteralContainer -> container shouldBeInstanceOf LiteralContainer::class
        is KeywordContainer -> container shouldBeInstanceOf KeywordContainer::class
        is SymbolContainer -> container shouldBeInstanceOf SymbolContainer::class
    }

    when (container) {
        is LiteralContainer -> {
            if (shouldBe is LiteralContainer)
                container.literal shouldBe shouldBe.literal
        }
        is KeywordContainer -> {
            if (shouldBe is KeywordContainer)
                container.keyword shouldBe shouldBe.keyword
        }
        is SymbolContainer -> {
            if (shouldBe is SymbolContainer)
                container.symbol shouldBe shouldBe.symbol
        }
    }
}

private fun Literal.toContainer() = LiteralContainer(this)
private fun Keyword.toContainer() = KeywordContainer(this)
private fun Symbol.toContainer() = SymbolContainer(this)