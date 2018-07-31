package com.luzon.fsm

//TODO: Helper class to deal with inputs and exiting with possible outputs at a specific location; to consume the correct input into a token.
class FSMachine<T>(statesList: List<State<T>>) {
    constructor(root: State<T>) : this(mutableListOf(root))

    private val states = statesList.toMutableList()

    init {
        updateEpsilons()
    }

    companion object {
        fun <T> fromRegex(str: String) = FSMachine(RegexScanner<T>(str).toFSM())
    }

    private fun updateEpsilons(): Boolean {
        val epsilons = states.map { it.acceptEpsilons() }.merge().toMutableList()

        do {
            val moreEpsilons = epsilons
                    .map { it.acceptEpsilons() }.merge()
                    .filter { !epsilons.contains(it) }
            epsilons.addAll(moreEpsilons)
        } while (moreEpsilons.isNotEmpty())

        states.addAll(epsilons)
        return epsilons.isNotEmpty()
    }

    fun accept(char: Char): Boolean {
        val newStates = states.map { it.accept(char) }.merge()
        states.clear()
        states.addAll(newStates)

        return updateEpsilons() || newStates.isNotEmpty()
    }

    fun isRunning() = states.isNotEmpty()

    fun isAccepting() = states.any { it.isAccepting() }

    fun getCurrentOutput(): List<T> = states.filter { it.isAccepting() }.map { it.output!! }.distinct()

    //TODO: Temporary solution (Not very efficient, can have many duplicate states with transitions)
    fun merge(other: FSMachine<T>) = FSMachine(states + other.states) //TODO: Set accept output for each side of the machine here then set any accept states to the appropriate value.

    fun getStateCount() = states.count()
}

class RegexScanner<T>(private val regex: String) { //TODO: Backslash metacharacters
    private var current = 0
    private val root = State<T>()
    private var endState = root
    private var metaScope = root
    private var scopeChange = false
    private var orState: State<T>? = null
    private var orEndState: State<T>? = null
    private var afterOr = false

    companion object {
        private const val END_CHAR: Char = '\n'
        private const val metaCharacters = "*+?|"
    }

    fun toFSM(): State<T> {
        while (!atEnd()) {
            val char = peek()

            val startEnd = when (char) {
                '[' -> orBlock()
                '(' -> parenthesis()
                '{' -> TODO("Repetitions -> Relies on metaScope too") //TODO: Might not need this for my language specifically, but should implement if I want this to be a full regex parser
                in metaCharacters -> metaCharacter()
                else -> char()
            }

            endState = startEnd.second
            endState.removeAccept()

            if (afterOr) { //TODO: Re-look at this and the or function, as endState is adding an epsilon twice sometimes.
                endState.addEpsilonTransition(orEndState!!)
                afterOr = false
            }

            if (scopeChange) {
                metaScope = endState
                scopeChange = false
            }
        }

        endState.forceAccept = true

        return root
    }

    fun advance(): Char {
        val char = peek()
        if (!atEnd()) current++
        return char
    }

    fun isNormalChar() = peek() !in "*+?|(){}[]\n"

    fun peek() = if (current < regex.length) regex[current] else END_CHAR

    fun atEnd() = current >= regex.length

    fun char(): Pair<State<T>, State<T>> {
        val charEnd = State<T>(forceAccept = true)
        endState.addTransition(charPredicate(advance()), charEnd)
        return endState to charEnd
    }

    fun orBlock(): Pair<State<T>, State<T>> {
        val end = State<T>(forceAccept = true)
        var transitionPredicate: (Char) -> Boolean = { false }

        advance() //Consume '['

        do {
            val char = advance()
            transitionPredicate = if (peek() == '-') { //Is Range
                advance() //Consume '-'
                orPredicate(transitionPredicate, rangePredicate(char, advance()))
            } else orPredicate(transitionPredicate, charPredicate(char))
        } while (char != ']' && char != END_CHAR)

        advance() //Consume ']'

        endState.addTransition(transitionPredicate, end)
        return endState to end
    }

    fun parenthesis(): Pair<State<T>, State<T>> {
        advance() //Consume '('
        val scanner = RegexScanner<T>(advanceUntil(')'))
        val states = scanner.toFSM()
        endState.addEpsilonTransition(states)
        metaScope = states //TODO: scopeChange = true?

        return states to scanner.endState
    }

    fun metaCharacter(): Pair<State<T>, State<T>> = when (advance()) {
        '|' -> or()
        '*' -> asterisk()
        '+' -> newPlus()
        '?' -> newQuestion() //TODO: Some of these may need to change the metaScope
        else -> TODO("Not a valid metaCharacter (Should never happen). Throw Exception, or log error")
    }

    fun or(): Pair<State<T>, State<T>> {
        scopeChange = true
        if (orState == null) { //First or in the regex
            val or = State<T>()
            orState = or
            root.addEpsilonTransition(orState!!)
        }

        orState!!.addEpsilonTransition(metaScope)

        if (orState == null) { //First or
            orState = State()
            orEndState = State()
        }

        orState!!.addEpsilonTransition(metaScope)
        metaScope.findLeaves()[0].addEpsilonTransition(orEndState!!)

        afterOr = true
        return orState!! to orEndState!!
    }

    fun asterisk(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        metaScope.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    fun newPlus(): Pair<State<T>, State<T>> {
        val newEndState = State<T>(forceAccept = true)

        endState.addEpsilonTransition(metaScope)
        endState.addEpsilonTransition(newEndState)

        return metaScope to newEndState
    }

    fun newQuestion(): Pair<State<T>, State<T>> {
        TODO()
    }

    private fun advanceUntil(char: Char) = advanceUntil { it == char }

    private fun advanceUntil(pred: (Char) -> Boolean): String {
        val sb = StringBuilder()

        do {
            val char = advance()
            if (!pred(char)) sb.append(char) //Ensure the final character isn't included. i.e. ')'
        } while (!pred(char) && char != END_CHAR) //TODO: Error if hits END_CHAR rather than the predicate
        //TODO: Could check atEnd() rather than char != END_CHAR

        return sb.toString()
    }
}

fun rangePredicate(start: Char, end: Char): (Char) -> Boolean = { it in start..end }
fun charPredicate(c: Char): (Char) -> Boolean = { it == c }
fun orPredicate(first: (Char) -> Boolean, second: (Char) -> Boolean): (Char) -> Boolean = { first(it) || second(it) }

fun <T> List<List<T>>.merge() = fold(emptyList<T>()) { acc, stateList -> acc + stateList }
