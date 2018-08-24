
import com.luzon.fsm.FSMachine
import com.luzon.fsm.State
import com.luzon.kodein
import com.luzon.utils.predicate
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.nio.file.Paths

object FSMTest : Spek({
    given("a finite state machine") {
        on("a simple transition") {
            val root = State<Char, Int>()
            root.addTransition('A'.predicate(), State())
            val machine = FSMachine(root)
            it("should accept to the next state successfully") {
                machine.accept('A')
                machine.isRunning() shouldBe true
            }
        }

        on("an epsilon transition") {
            val root = State<Char, Int>()
            val otherState = State<Char, Int>()
            otherState.addTransition('A'.predicate(), State())
            root.addEpsilonTransition(otherState)
            val machine = FSMachine(root)

            it("should end with 1 state") {
                machine.accept('A')
                machine.getStateCount() shouldBe 1
            }
        }

        on("a state") {
            it("finds leaf states correctly") {
                val root = State<Char, Int>()
                for (i in 1..5) root.addEpsilonTransition(State())
                root.findLeaves().size shouldBe 5
            }
        }
    }

    given("a regex parser") {
        on("a character block") {
            val machine = regex("ABCD")

            it("should accept correct values for ABCD") {
                machine.accept("ABCD")
                machine.isAccepting() shouldBe true
            }

            it("should not accept invalid values for ABCD") {
                machine.reset()
                machine.accept("AD")
                machine.isAccepting() shouldBe false
            }
        }

        on("an or block") {
            val machine = regex("[ABD-Za-z]")

            it("should accept correct values for [ABD-Za-z]") {
                "ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".forEach {
                    machine.reset()
                    machine.accept(it)
                    machine.isAccepting() shouldBe true
                }
            }

            it("should not accept invalid values for [ABD-Za-z]") {
                "C0123456789".forEach {
                    machine.reset()
                    machine.accept(it)
                    machine.isRunning() shouldBe false
                }
            }
        }

        on("parenthesis") {
            val machine = regex("(ABCD)")

            it("should accept correct values for (ABCD)") {
                machine.accept("ABCD")
                machine.isAccepting() shouldBe true
            }

            it("should not accept invalid values for (ABCD)") {
                machine.reset()
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
            val machine = regex("A+")

            it("should accept multiple A's for A+") {
                for (i in 1..5) machine.accept('A')

                machine.isAccepting() shouldBe true
            }

            it("it should not accept no input") {
                machine.reset()

                machine.isAccepting() shouldBe false
            }
        }

        on("a question regex") {
            val machine = regex("A?")

            it("should accept a single A for A?") {
                machine.accept('A')

                machine.isAccepting() shouldBe true
            }

            it("should accept for no input") {
                machine.reset()

                machine.isAccepting() shouldBe true
            }
        }

        on("an or regex") {
            it("should accept either A or B for A|B") {
                val machine = regex("A|B")

                machine.accept('A')
                machine.isAccepting() shouldBe true

                machine.reset()

                machine.accept('B')
                machine.isAccepting() shouldBe true
            }

            it("should accept either AB or CD for AB|CD") {
                val machine = regex("AB|CD")

                machine.accept("AB")
                machine.isAccepting() shouldBe true

                machine.reset()

                machine.accept("CD")
                machine.isAccepting() shouldBe true
            }
        }

        on("a complex regex") {
            val machine = regex("(AB|CD)*")

            it("should pass with no inputs") {
                machine.isAccepting() shouldBe true
            }

            it("should pass with multiple ABs") {
                machine.reset()

                "AB".repeat(5).forEach { machine.accept(it) }

                machine.isAccepting() shouldBe true
            }

            it("should pass with ABCD") {
                machine.reset()

                "ABCD".forEach { machine.accept(it) }

                machine.isAccepting() shouldBe true
            }
        }
    }

    given("a merged state machine") {
        val machine1 = regex("AB")
        val machine2 = regex("CD")

        it("should accept either AB or CD") {
            val merged = machine1.merge(machine2)

            merged.accept("AB")
            merged.isAccepting() shouldBe true

            merged.reset()
            merged.accept("CD")
            merged.isAccepting() shouldBe true

            merged.accept("A")
            merged.isAccepting() shouldBe false
        }

        val machine3 = regex("EF")

        it("should work with more merged machines") {
            val merged = FSMachine.merge(machine1, machine2, machine3)

            merged.accept("AB")
            merged.isAccepting() shouldBe true

            merged.reset()
            merged.accept("CD")
            merged.isAccepting() shouldBe true

            merged.reset()
            merged.accept("EF")
            merged.isAccepting() shouldBe true

            merged.accept("A")
            merged.isAccepting() shouldBe false
        }
    }

    given("examples with a regex parser") {
        val json = Files.readAllLines(Paths.get(FSMTest::class.java.getResource("regex_examples.json").toURI())).joinToString(" ")
        val paramType = Types.newParameterizedType(List::class.java, RegexIOTest::class.java)
        val adapter = Moshi.Builder().build().adapter<List<RegexIOTest>>(paramType)
        val examples = adapter.fromJson(json)!!

        examples.forEach { (regex, tests) ->
            on("the regular expression $regex") {
                val machine = regex(regex)
                tests.forEach { (test, result) ->
                    val emptyInput = test.isEmpty()
                    it("should receive $result for isAccepting with the input $test") {
                        machine.reset()
                        if (!emptyInput) machine.accept(test)
                        machine.isAccepting() shouldBe result
                    }
                }
            }
        }
    }
})

private fun regex(regex: String): FSMachine<Char, Int> = FSMachine.fromRegex(regex)

//Just converts the old format of "<regex> (<input> <expected output>)+"
private fun fromTestTxtToJson(): String {
    val fileText = Files.readAllLines(Paths.get(FSMTest::class.java.getResource("regex_examples.txt").toURI()))
    val paramType = Types.newParameterizedType(List::class.java, RegexIOTest::class.java)
    val moshi: Moshi by kodein.instance()
    val adapter = moshi.adapter<List<RegexIOTest>>(paramType)

    val tests = fileText.map { line ->
        val split = line.split(" ")
        val regex = split[0]
        val tests = split.subList(1, split.size).zipWithNext().map {
            (if (it.first == "<none>") "" else it.first) to it.second.toBoolean()
        }.toMap()

        RegexIOTest(regex, tests)
    }

    return adapter.toJson(tests)
}