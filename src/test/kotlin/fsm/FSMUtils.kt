package fsm

import com.luzon.fsm.FiniteStateMachine

fun FiniteStateMachine<Char, Int>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}
