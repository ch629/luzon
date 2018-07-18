import com.luzon.fsm.FSMachine
import com.luzon.fsm.RegexScanner
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object RegexTest : Spek({
    given("a regex parser") {
        on("an or block") {
            it("should return ") {
                val scanner = RegexScanner<Int>("[ABD-Za-z]")
                val states = scanner.orBlock()

                "ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".forEach {
                    FSMachine(states).accept(it) shouldBe true
                }
            }
        }
    }
})