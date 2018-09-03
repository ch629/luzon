package com.luzon.parser

import com.luzon.fsm.FSM
import com.luzon.fsm.MetaScanner
import com.luzon.fsm.State
import com.luzon.lexer.Token
import com.luzon.lexer.Token.*
import com.luzon.lexer.Tokenizer
import java.util.*

class TokenScanner(tokens: List<TokenScannerAlphabet>) : MetaScanner<TokenScannerAlphabet, (List<Token>) -> ASTNode>(tokens, TokenScannerAlphabet.Meta(MetaCharacter.NONE)) {
    private val helper = TokenScannerHelper()

    companion object {
        fun fromList(values: List<Any>) = fromList(*values.toTypedArray())

        fun fromList(vararg values: Any) = TokenScanner(values.map {
            when (it) {
                is Token -> TokenScannerAlphabet.Token(it)
                is TokenEnum -> TokenScannerAlphabet.Token(it.toToken())
                is MetaCharacter -> TokenScannerAlphabet.Meta(metaCharacter = it)
                is TokenScannerAlphabet -> it
                else -> TokenScannerAlphabet.Meta(MetaCharacter.NONE)
            }
        })
    }

    override val orPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.OR }
    override val kleeneStarPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.KLEENE_STAR }
    override val kleenePlusPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.KLEENE_PLUS }
    override val optionalPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.OPTIONAL }
    override val startGroupPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.START_GROUP }
    override val endGroupPredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.END_GROUP }
    override val escapePredicate: (TokenScannerAlphabet) -> Boolean
        get() = { it == MetaCharacter.ESCAPE }

    override fun customCharacters(char: TokenScannerAlphabet): StatePair<TokenScannerAlphabet, (List<Token>) -> ASTNode>? = when (char) {
        is TokenScannerAlphabet.NonTerminal -> nonTerminal(char)
        else -> super.customCharacters(char) //null?
    }

    private fun nonTerminal(nonTerminal: TokenScannerAlphabet.NonTerminal): StatePair<TokenScannerAlphabet, (List<Token>) -> ASTNode> {
        val newState = State<TokenScannerAlphabet, (List<Token>) -> ASTNode>()

        newState.onEnter += {
            helper.nonTerminal(nonTerminal.name)
        }

        return StatePair(newState, newState)
    }

    override fun createScanner(text: List<TokenScannerAlphabet>) = TokenScanner(text)
}

sealed class TokenScannerAlphabet {
    override fun equals(other: Any?) = when (other) {
        null -> false
        is MetaCharacter -> this is Meta && metaCharacter == other
        is Meta -> this is Meta && metaCharacter == other.metaCharacter
        is TokenEnum -> this is Token && token.tokenEnum == other
        else -> false
    }

    override fun hashCode() = javaClass.hashCode()

    class Meta(val metaCharacter: MetaCharacter) : TokenScannerAlphabet()
    class Token(val token: com.luzon.lexer.Token) : TokenScannerAlphabet()
    class NonTerminal(val name: String) : TokenScannerAlphabet()
}

//TODO: Potentially use this in a pair-like type for all MetaScanners, and just convert '|' into OR
enum class MetaCharacter {
    OR,
    KLEENE_STAR,
    KLEENE_PLUS,
    OPTIONAL,
    START_GROUP,
    END_GROUP,
    ESCAPE,
    NONE
}

object NonTerminalHandler {
    private val nonTerminals = hashMapOf<String, State<TokenScannerAlphabet, (List<Token>) -> ASTNode>>()

    fun addNonTerminal(name: String, scannerContainers: List<TokenScannerAlphabet>) {
        val newStates = TokenScanner(scannerContainers).toFSM()

        if (nonTerminals.containsKey(name)) nonTerminals[name]!!.addEpsilonTransition(newStates)
        else {
            val root = State<TokenScannerAlphabet, (List<Token>) -> ASTNode>()
            root.addEpsilonTransition(newStates)
            nonTerminals[name] = root
        }
    }

    fun getFSM(name: String) = FSM(nonTerminals[name] ?: State())
}

class TokenScannerHelper {
    private val stateStack = Stack<FSM<TokenScannerAlphabet, (List<Token>) -> ASTNode>>()
    private var currentMachine: FSM<TokenScannerAlphabet, (List<Token>) -> ASTNode>? = null

    fun nonTerminal(name: String) {
        stateStack.push(currentMachine)
        currentMachine = NonTerminalHandler.getFSM(name)
    }
}

//TODO: Maybe define all nodes in terms of Tokens and MetaCharacters, then convert a string input into
//TODO: Need some sort of way to make these recursive, so I can define something like a argument list; which can be used within a function call
fun main(args: Array<String>) {
    val scanner = TokenScanner.fromList(
            Keyword.FOR,
            Symbol.L_PAREN,
            Keyword.VAR,
            Literal.IDENTIFIER,
            Keyword.IN,
            Literal.INT,
            Symbol.RANGE,
            Literal.INT,
            Symbol.R_PAREN
    ) //for(var i in 0..5)

    val forLoop = "for lParen var identifier in literal:int range literal:int rParen <block>"
    val param = "identifier type identifier"
    val paramList = "<param> | <param> comma <paramList>"
    val funDecl = "fun lParen <paramList>? rParen (type identifier)? <block>"

    val machine = FSM(scanner.toFSM())
    val code = "for(var i in 0..5)"
    val tokenizer = Tokenizer(code)

    println(tokenizer.tokensAsString())
    val tokens = tokenizer.findTokens()

    tokens.forEach {
        machine.accept(TokenScannerAlphabet.Token(token = it))
    }

    println(machine.isAccepting())
}