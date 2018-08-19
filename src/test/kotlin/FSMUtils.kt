import com.luzon.fsm.FSMachine

fun FSMachine<*>.accept(input: String) {
    input.forEach {
        accept(it)
    }

}