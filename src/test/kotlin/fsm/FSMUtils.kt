package fsm

import com.luzon.fsm.FSM

fun FSM<Char, Int>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}
