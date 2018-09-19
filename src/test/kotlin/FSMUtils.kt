import com.luzon.fsm.FSM

fun FSM<Char>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}