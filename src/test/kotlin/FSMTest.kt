import com.luzon.fsm.FSMachine
import com.luzon.fsm.State
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object FSMTest : Spek({
    given("a finite state machine") {
        on("a simple transition") {
            val root = State<Int>()
            root.addTransition({ it == 'A' }, State())
            val machine = FSMachine(root)
            it("should accept to the next state successfully") {
                machine.accept('A')
                machine.isRunning() shouldBe true
            }
        }

        on("an epsilon transition") {
            val root = State<Int>()
            val otherState = State<Int>()
            otherState.addTransition({ it == 'A' }, State())
            root.addEpsilonTransition(otherState)
            val machine = FSMachine(root)

            it("should end with 1 state") {
                machine.accept('A')
                machine.getStateCount() shouldBe 1
            }
        }

        on("a state") {
            it("finds leaf states correctly") {
                val root = State<Int>()
                for (i in 1..5) root.addEpsilonTransition(State())
                root.findLeaves().size shouldBe 5

            }
        }
    }

    given("a regex parser") {
        on("a character block") {
            val regex = "ABCD"
            it("should accept correct values for ABCD") {
                val machine = regex(regex)
                regex.forEach {
                    machine.accept(it)
                    machine.isRunning() shouldBe true
                }
            }

            it("should not accept invalid values for ABCD") {
                val machine = regex(regex)
                machine.accept('A')
                machine.accept('D')
                machine.isRunning() shouldBe false
            }
        }

        on("an or block") {
            val orBlockRegex = "[ABD-Za-z]"

            it("should accept correct values for [ABD-Za-z]") {
                "ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".forEach {
                    val machine = regex(orBlockRegex)
                    machine.accept(it)
                    machine.isRunning() shouldBe true
                }
            }

            it("should not accept invalid values for [ABD-Za-z]") {
                "C0123456789".forEach {
                    val machine = regex(orBlockRegex)
                    machine.accept(it)
                    machine.isRunning() shouldBe false
                }
            }
        }

        on("parenthesis") {
            val parenthesisRegex = "(ABCD)"
            it("should accept correct values for (ABCD)") {
                val machine = regex(parenthesisRegex)
                "ABCD".forEach {
                    machine.accept(it)
                }
                machine.isRunning() shouldBe true
            }

            it("should not accept invalid values for (ABCD)") {
                val machine = FSMachine.fromRegex<Int>(parenthesisRegex)
                machine.accept('D')
                machine.isRunning() shouldBe false
            }
        }

        on("an asterisk regex") {
            it("should accept multiple A's for A*") {
                val machine = regex("A*")

                for (i in 1..5) machine.accept('A')

                machine.isRunning() shouldBe true
            }

            it("should accept multiple AB's for AB*") {
                val machine = regex("AB*")

                for (i in 1..5) {
                    machine.accept('A')
                    machine.accept('B')
                }

                machine.isRunning() shouldBe true
            }
        }
    }
})

private fun regex(regex: String): FSMachine<Int> = FSMachine.fromRegex(regex)