import com.luzon.lexer.TokenRegexJson
import org.amshove.kluent.shouldBeGreaterThan
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

object TokenTest : Spek({
    given("a token file") {
        it("should load correctly into a TokenRegexJson") {
            val tokens = TokenRegexJson.fromJson()

            tokens.keywords.size shouldBeGreaterThan 0
        }
    }
})