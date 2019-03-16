package fsm

import com.luzon.fsm.FSM
import com.luzon.fsm.State
import com.luzon.utils.equalPredicate
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeGreaterThan
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Files
import java.nio.file.Paths

object FSMTest : Spek({
    describe("a finite state machine") {
        context("a simple transition") {
            val root = State<Char, Unit>()
            root.addTransition('A'.equalPredicate(), State())
            val machine = FSM(listOf(root))
            it("should accept to the next state successfully") {
                machine.accept('A')
                machine.running shouldBe true
            }
        }

        context("an epsilon transition") {
            val root = State<Char, Unit>()
            val otherState = State<Char, Unit>()
            otherState.addTransition('A'.equalPredicate(), State())
            root.addEpsilon(otherState)
            val machine = FSM(listOf(root))

            it("should end with 1 state") {
                machine.accept('A')
                machine.stateCount shouldBe 1
            }
        }

        context("chained epsilon transitions") {
            val root = State<Char, Unit>()
            val state1 = State<Char, Unit>()
            val state2 = State<Char, Unit>()
            val state3 = State<Char, Unit>()
            val state4 = State<Char, Unit>(forceAccept = true)

            root.addEpsilon(state1)
            state1.addEpsilon(state2)
            state2.addTransition({ true }, state3)
            state3.addEpsilon(state4)
            state2.addEpsilon(state4)

            val machine = FSM(listOf(root))
            machine.stateCount shouldBeGreaterThan 1
            machine.accepting shouldBe true

            machine.accept('a')
            machine.stateCount shouldBeGreaterThan 1
            machine.accepting shouldBe true
        }

//        context("a state") {
//            it("finds leaf states correctly") {
//                val root = State<Char, Unit>()
//                for (i in 1..5) root.addEpsilon(State())
//                root.leaves.size shouldBe 5
//            }
//        }
    }

    describe("a regex parser") {
        context("a character block") {
            val machine = regex("ABCD")

            it("should accept correct values for ABCD") {
                machine.accept("ABCD")
                machine.accepting shouldBe true
            }

            it("should not accept invalid values for ABCD") {
                machine.reset()
                machine.accept("AD")
                machine.accepting shouldBe false
            }
        }

        context("an or block") {
            val machine = regex("[ABD-Za-z]")

            it("should accept correct values for [ABD-Za-z]") {
                "ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".forEach {
                    machine.reset()
                    machine.accept(it)
                    machine.accepting shouldBe true
                }
            }

            it("should not accept invalid values for [ABD-Za-z]") {
                "C0123456789".forEach {
                    machine.reset()
                    machine.accept(it)
                    machine.running shouldBe false
                }
            }
        }

        context("parenthesis") {
            val machine = regex("(ABCD)")

            it("should accept correct values for (ABCD)") {
                machine.accept("ABCD")
                machine.accepting shouldBe true
            }

            it("should not accept invalid values for (ABCD)") {
                machine.reset()
                machine.accept('D')
                machine.running shouldBe false
            }
        }

        context("an asterisk regex") {
            it("should accept multiple A's for A*") {
                val machine = regex("A*")

                machine.accept("AAAAA")

                machine.accepting shouldBe true
            }

            it("should accept no input") {
                val machine = regex("A*")

                machine.accepting shouldBe true
            }

            it("should accept multiple AB's for AB*") {
                val machine = regex("AB*")

                "AB".repeat(5).forEach {
                    machine.accept(it)
                }

                machine.accepting shouldBe true
            }
        }

        context("a plus regex") {
            val machine = regex("A+")

            it("should accept multiple A's for A+") {
                for (i in 1..5) machine.accept('A')

                machine.accepting shouldBe true
            }

            it("it should not accept no input") {
                machine.reset()

                machine.accepting shouldBe false
            }
        }

        context("a question regex") {
            val machine = regex("A?")

            it("should accept a single A for A?") {
                machine.accept('A')

                machine.accepting shouldBe true
            }

            it("should accept for no input") {
                machine.reset()

                machine.accepting shouldBe true
            }
        }

        context("an or regex") {
            it("should accept either A or B for A|B") {
                val machine = regex("A|B")

                machine.accept('A')
                machine.accepting shouldBe true

                machine.reset()

                machine.accept('B')
                machine.accepting shouldBe true
            }

            it("should accept either AB or CD for AB|CD") {
                val machine = regex("AB|CD")

                machine.accept("AB")
                machine.accepting shouldBe true

                machine.reset()

                machine.accept("CD")
                machine.accepting shouldBe true
            }
        }

        context("a complex regex") {
            val machine = regex("(AB|CD)*")

            it("should pass with no inputs") {
                machine.accepting shouldBe true
            }

            it("should pass with multiple ABs") {
                machine.reset()

                "AB".repeat(5).forEach { machine.accept(it) }

                machine.accepting shouldBe true
            }

            it("should pass with ABCD") {
                machine.reset()

                "ABCD".forEach { machine.accept(it) }

                machine.accepting shouldBe true
            }
        }
    }

    describe("a merged state machine") {
        val machine1 = regex("AB")
        val machine2 = regex("CD")

        it("should accept either AB or CD") {
            val merged = machine1.merge(machine2)

            merged.accept("AB")
            merged.accepting shouldBe true

            merged.reset()
            merged.accept("CD")
            merged.accepting shouldBe true

            merged.accept("A")
            merged.accepting shouldBe false
        }

        val machine3 = regex("EF")

        it("should work with more merged machines") {
            val merged = FSM.merge(machine1, machine2, machine3)

            merged.accept("AB")
            merged.accepting shouldBe true

            merged.reset()
            merged.accept("CD")
            merged.accepting shouldBe true

            merged.reset()
            merged.accept("EF")
            merged.accepting shouldBe true

            merged.accept("A")
            merged.accepting shouldBe false
        }
    }

    describe("examples with a regex parser") {
        val json = Files.readAllLines(Paths.get(FSMTest::class.java.getResource("regex_examples.json").toURI())).joinToString(" ")
        val paramType = Types.newParameterizedType(List::class.java, RegexIOTest::class.java)
        val adapter = Moshi.Builder().build().adapter<List<RegexIOTest>>(paramType)
        val examples = adapter.fromJson(json)!!

        examples.forEach { (regex, tests) ->
            context("the regular expression $regex") {
                val machine = regex(regex)
                tests.forEach { (test, result) ->
                    val emptyInput = test.isEmpty()
                    it("should receive $result for.accepting with the input $test") {
                        machine.reset()
                        if (!emptyInput) machine.accept(test)
                        machine.accepting shouldBe result
                    }
                }
            }
        }
    }
})

private fun regex(regex: String): FSM<Char, Int> = FSM.fromRegex(regex)