package com.luzon.parser

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.luzon.fsm.FSM
import com.luzon.fsm.State
import com.luzon.lexer.Token
import com.luzon.lexer.Token.Keyword.VAL
import com.luzon.lexer.Token.Keyword.VAR
import com.luzon.lexer.Token.Literal.*
import com.luzon.utils.merge
import com.luzon.utils.replaceWith
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

private typealias TokenPredicate = (Token) -> Boolean
private typealias ToState<T> = Pair<T, TokenState>

//TODO: Create a DSL for generating the parser ASTNode classes.
class TokenState {
    val nonTerminalTransitions = listOf<ToState<String>>()
    val normalTransitions = listOf<ToState<TokenPredicate>>()
    val epsilonTransitions = listOf<TokenState>() //TODO: Might not need epsilons, but I'll keep it here just in case

    fun accept(token: Token): List<TokenState> {
        TODO()
    }

    fun accept(nonTerminal: String): List<TokenState> {
        TODO()
    }

    fun acceptEpsilons(): List<TokenState> {
        TODO()
    }
}

//TODO: Look at these top classes and try to update using tryConstructorArguments.
class TokenFSM(val name: String, val output: KClass<*>? = null) {
    val states = mutableListOf<TokenState>()
    val originalStates = states.toList()
    val waitingTransitions = mutableListOf<(String) -> TokenState?>() //TODO: Null TokenState object?
    //TODO: I need to figure out whether this should take an ASTNode
    //TODO: If this was a string, I could just compare whether the correct non-terminal was used to determine whether to accept waiting state
    var atBeginning = true

    fun accept(token: Token): Boolean { //TODO: If there are self non-terminals, they need to be added to wait
        atBeginning = false
        val acceptedStates = states.map { it.accept(token) }.merge()
        states.forEach {
            val transitions = it.nonTerminalTransitions
                    .filter { it.first == name } //self calls -> Also check atBeginning?
                    .map { (str, state) ->
                        val ret: (String) -> TokenState? = { if (it == str) state else null } //TODO: I need a way to create an ASTNode into data somewhere
                        ret
                    }
            waitingTransitions.addAll(transitions)
            //TODO: Add to waiting
        }
        return states.replaceWith(acceptedStates) && acceptedStates.isNotEmpty()
    }

    fun acceptWaiting(nonTerminal: String): Boolean {
        return waitingTransitions.map {
            val state = it(nonTerminal)
            return if (state != null) states.add(state)
            else false
        }.any { it } //Successfully accepted at least one
        //TODO: Clear the waiting transitions if failed? -> Or just remove the waiting transitions that pass?
    }

    fun hasTerminalOption(): Boolean {
        TODO()
    }

    fun getNonTerminals(): List<TokenFSM> {
        TODO()
    }

    fun reset() {
        states.replaceWith(originalStates)
        waitingTransitions.clear()
    }

    fun merge(other: TokenFSM): TokenFSM {
        TODO()
    }
}

object NonTerminalHandler {
    private val nonTerminals = hashMapOf<String, TokenFSM>()

    fun getFSM(name: String): TokenFSM? {
        return nonTerminals[name]
    }
}

class TokenFSMManager { //TODO: Deal with only having one of each non-terminal FSM running at the same point. -> This would be easier using a List of FSMs, which could store whether it is at the beginning
    private val fsmStack = Stack<List<TokenFSM>>()
    private val activeFSM get() = if (fsmStack.isNotEmpty()) fsmStack.peek() else null
    //TODO: This probably has to be a list of FSMs as multiple non-terminals could be options which have to be checked
    //TODO: Unless I just make one FSM out of multiple?

    fun pop(node: ASTNode, nonTerminal: String) {
        //TODO: Add node to data somewhere?
        if (fsmStack.isNotEmpty()) fsmStack.pop()
        activeFSM?.forEach { it.acceptWaiting(nonTerminal) }
    }

    fun push(nonTerminal: String) {
        val fsm = NonTerminalHandler.getFSM(nonTerminal)
        if (fsm != null) fsmStack.push(listOf(fsm))
    }

    fun push(vararg nonTerminals: String) {
        val fsm = nonTerminals
                .map { NonTerminalHandler.getFSM(it) }
                .filter { it != null }
                .map { it!! }

        fsmStack.push(fsm)
    }

    fun accept(token: Token) { //TODO: Maybe draw another example using the lists of FSMs?
        //TODO: I need a way to look at the transitions needed to move on
        //If there is only a non-terminal then I need to push the non-terminal
        //This needs to be repeated until I have a terminal option
        //What to do if there is a non-terminal and a terminal?

        //TODO: Loop until I hit a non-terminal option?
    }

    //TODO: Where do I create the ASTNodes to create each other?
}

sealed class ASTCreatorError(val msg: String) {
    class InvalidTokenFound(found: String, expected: String) : ASTCreatorError("Found token $found when expecting $expected")
}

data class TokenNode<T>(val value: T, val type: Token.TokenEnum) : ASTNode {
    constructor(pair: Pair<T, Token.TokenEnum>) : this(pair.first, pair.second)

    fun isType(tokenType: Token.TokenEnum) = when (tokenType) {
        DOUBLE -> value is Double
        FLOAT -> value is Float
        INT -> value is Int
        STRING -> value is String && type == STRING
        CHAR -> value is Char
        BOOLEAN -> value is Boolean
        IDENTIFIER -> value is String && type == IDENTIFIER
        else -> false
    }
}

data class IdentifierNode(val name: String) : ASTNode
data class VarValNode(val token: Token.TokenEnum) : ASTNode {
    fun isVar() = token == VAR
    fun isVal() = token == VAL

    companion object {
        fun fromToken(token: Token.TokenEnum): Either<IllegalArgumentException, VarValNode> {
            return when (token) {
                VAR,
                VAL -> Right(VarValNode(token))
                else -> Left(IllegalArgumentException("Should either be a VAR or a VAL but was a $token"))
            }
        }
    }
}

//TODO: Make my own exceptions? or an error type?
private fun Token.toNode(): Either<IllegalArgumentException, ASTNode> {
    return if (tokenEnum is Token.Literal) {
        Right(TokenNode(when (tokenEnum) {
            DOUBLE -> data.toDouble() to DOUBLE
            FLOAT -> data.toFloat() to FLOAT
            INT -> data.toInt() to INT
            STRING -> makeString(data) to STRING
            CHAR -> makeChar(data) to CHAR
            BOOLEAN -> data.toBoolean() to BOOLEAN
            IDENTIFIER -> data to IDENTIFIER
        }))
    } else Left(IllegalArgumentException("Not a literal"))
}

private fun makeChar(string: String) = makeString(string)[0]
private fun makeString(string: String) = string.substring(1, string.length - 1)

fun getConstructorParameters(clazz: KClass<*>) = clazz.constructors.map { con ->
    con.parameters.map { param ->
        param.type.classifier as KClass<*>
    }
}

fun KClass<*>.constructorsToFSM(): FSM<KClass<*>, KFunction<KClass<*>>> {
    val root = State<KClass<*>, KFunction<KClass<*>>>()
    var pointer = root

    constructors.forEach { con ->
        con.parameters.forEach { param ->
            val newState = State<KClass<*>, KFunction<KClass<*>>>()

            pointer.addTransition({ it.isSubclassOf(param.type.classifier as KClass<*>) }, newState)
            pointer = newState
        }

        pointer.output = con as KFunction<KClass<*>>
        pointer = root
    }

    return FSM(root)
}

private val constructorFSMCache = hashMapOf<KClass<*>, FSM<KClass<*>, KFunction<KClass<*>>>>()
fun <T : Any> tryConstructorArguments(clazz: KClass<T>, vararg args: Any): T? { //TODO: Use Either here? -> Rather than nullable
    if (!constructorFSMCache.containsKey(clazz)) constructorFSMCache[clazz] = clazz.constructorsToFSM()
    val fsm = constructorFSMCache[clazz]!!.copy()

    args.forEach { fsm.accept(it::class) }

    return (fsm.getCurrentOutput().firstOrNull() as KFunction<T>?)?.call(*args)
}

inline fun <reified T : Any> tryConstructorArgs(vararg args: Any) = tryConstructorArguments(T::class, *args)

fun main(args: Array<String>) {
    //i: Int
    val decl = tryConstructorArgs<VariableDeclaration>("i", "Int")!!
    println("VariableDeclaration: ${decl.name}: ${decl.type!!}")

    //b = 5
    val decl2 = tryConstructorArgs<VariableDeclaration>("b", LiteralExpression(Token.Literal.INT.toToken("5")))!!
    println("VariableDeclaration: ${decl2.name}")

    getConstructorParameters(VariableDeclaration::class).forEach { conParams ->
        println(conParams.joinToString { it.simpleName!! })
    }
}