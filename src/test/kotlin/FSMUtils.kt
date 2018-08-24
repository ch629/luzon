import com.luzon.fsm.FSMachine

fun FSMachine<Char, *>.accept(input: String) {
    input.forEach {
        accept(it)
    }

}