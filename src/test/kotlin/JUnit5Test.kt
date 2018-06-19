import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object KotlinSpek : Spek({
    given("a calculator") {
        on("addition") {
            it("should return the result of adding the first number to the second number") {
                5 shouldEqual 5
            }
        }
    }
})