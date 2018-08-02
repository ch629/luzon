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
        //TODO: Test everything together
        on("a character block") {
            val regex = "ABCD"
            it("should accept correct values for ABCD") {
                val machine = regex(regex)
                regex.forEach {
                    machine.accept(it)
                    machine.isRunning() shouldBe true
                }
                machine.isAccepting() shouldBe true
            }

            it("should not accept invalid values for ABCD") {
                val machine = regex(regex)
                machine.accept("AD")
                machine.isRunning() shouldBe false
            }
        }

        on("an or block") {
            val orBlockRegex = "[ABD-Za-z]"

            it("should accept correct values for [ABD-Za-z]") {
                "ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".forEach {
                    val machine = regex(orBlockRegex)
                    machine.accept(it)
                    machine.isAccepting() shouldBe true
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
                machine.accept("ABCD")
                machine.isAccepting() shouldBe true
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

                machine.accept("AAAAA")

                machine.isAccepting() shouldBe true
            }

            it("should accept no input") {
                val machine = regex("A*")

                machine.isAccepting() shouldBe true
            }

            it("should accept multiple AB's for AB*") {
                val machine = regex("AB*")

                "AB".repeat(5).forEach {
                    machine.accept(it)
                }

                machine.isAccepting() shouldBe true
            }
        }

        on("a plus regex") {
            val plusRegex = "A+"
            it("should accept multiple A's for A+") {
                val machine = regex(plusRegex)

                for (i in 1..5) machine.accept('A')

                machine.isAccepting() shouldBe true
            }

            it("it should not accept no input") {
                val machine = regex(plusRegex)

                machine.isAccepting() shouldBe false
            }
        }

        on("a question regex") {
            val questionRegex = "A?"
            it("should accept a single A for A?") {
                val machine = regex(questionRegex)
                machine.accept('A')

                machine.isAccepting() shouldBe true
            }

            it("should accept for no input") {
                val machine = regex(questionRegex)

                machine.isAccepting() shouldBe true
            }
        }

        on("an or regex") {
            it("should accept either A or B for A|B") {
                val orRegex = "A|B"
                var machine = regex(orRegex)

                machine.accept('A')
                machine.isAccepting() shouldBe true

                machine = regex(orRegex)

                machine.accept('B')
                machine.isAccepting() shouldBe true
            }

            it("should accept either AB or CD for AB|CD") {
                val orRegex = "AB|CD"
                var machine = regex(orRegex)

                machine.accept("AB")
                machine.isAccepting() shouldBe true

                machine = regex(orRegex)

                machine.accept("CD")
                machine.isAccepting() shouldBe true
            }
        }

        on("a complex regex") {
            val reg = "(AB|CD)*"
            it("should pass with no inputs") {
                val machine = regex(reg)
                machine.isAccepting() shouldBe true
            }

            it("should pass with multiple ABs") {
                val machine = regex(reg)

                "AB".repeat(5).forEach { machine.accept(it) }

                machine.isAccepting() shouldBe true
            }

            it("should pass with ABCD") {
                val machine = regex(reg)

                "ABCD".forEach { machine.accept(it) }

                machine.isAccepting() shouldBe true
            }
        }
    }
})

private fun regex(regex: String): FSMachine<Int> = FSMachine.fromRegex(regex)

private fun FSMachine<Int>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}