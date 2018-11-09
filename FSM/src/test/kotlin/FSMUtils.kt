import com.luzon.fsm.IFsm

fun IFsm<Char>.accept(input: String) {
    input.forEach {
        accept(it)
    }
}