package com.luzon.parser

import com.luzon.fsm.FSM
import com.luzon.fsm.MetaScanner
import com.luzon.fsm.Scanner
import com.luzon.fsm.State
import com.luzon.lexer.Token
import com.luzon.lexer.Token.*
import com.luzon.lexer.Tokenizer
import com.luzon.utils.replaceWith

typealias ASTCreator = (List<ASTData>) -> ASTNode

class TokenScanner(tokens: List<TokenScannerAlphabet>) : MetaScanner<TokenScannerAlphabet, ASTCreator>(tokens, TokenScannerAlphabet.AlphabetMeta(MetaCharacter.NONE)) {
    private val resolver = ASTResolver(scanner = this)

    companion object {
        fun fromList(values: List<Any>) = fromList(*values.toTypedArray())

        fun fromList(vararg values: Any) = TokenScanner(values.map {
            when (it) {
                is Token -> TokenScannerAlphabet.AlphabetToken(it)
                is TokenEnum -> TokenScannerAlphabet.AlphabetToken(it.toToken())
                is MetaCharacter -> TokenScannerAlphabet.AlphabetMeta(metaCharacter = it)
                is TokenScannerAlphabet -> it
                else -> TokenScannerAlphabet.AlphabetMeta(MetaCharacter.NONE)
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

    override fun customCharacters(char: TokenScannerAlphabet) = when (char) {
        is TokenScannerAlphabet.AlphabetNonTerminal -> nonTerminal(char)
        else -> super.customCharacters(char) //null?
    }

    private fun nonTerminal(nonTerminal: TokenScannerAlphabet.AlphabetNonTerminal): StatePair<TokenScannerAlphabet, ASTCreator> {
        val newState = State<TokenScannerAlphabet, ASTCreator>()

        newState.onEnter += {
            resolver.nonTerminal(nonTerminal.name)
        }

        return StatePair(newState, newState)
    }

    override fun createScanner(text: List<TokenScannerAlphabet>) = TokenScanner(text)
}

sealed class TokenScannerAlphabet {
    override fun equals(other: Any?) = when (other) {
        null -> false
        is MetaCharacter -> this is AlphabetMeta && metaCharacter == other
        is AlphabetMeta -> this is AlphabetMeta && metaCharacter == other.metaCharacter
        is TokenEnum -> this is AlphabetToken && token.tokenEnum == other
        else -> false
    }

    override fun hashCode() = javaClass.hashCode()

    class AlphabetMeta(val metaCharacter: MetaCharacter) : TokenScannerAlphabet()
    class AlphabetToken(val token: Token) : TokenScannerAlphabet()
    class AlphabetNonTerminal(val name: String) : TokenScannerAlphabet()
}

//TODO: Potentially use this in a pair-like type for all MetaScanners, and just convert '|' into OR
enum class MetaCharacter {
    OR, KLEENE_STAR, KLEENE_PLUS,
    OPTIONAL, START_GROUP, END_GROUP,
    ESCAPE, NONE
}

object NonTerminalHandler {
    private val nonTerminals = hashMapOf<String, State<TokenScannerAlphabet, ASTCreator>>()

    fun addNonTerminal(name: String, scannerContainers: List<TokenScannerAlphabet>) {
        val newStates = TokenScanner(scannerContainers).toFSM()

        if (nonTerminals.containsKey(name)) nonTerminals[name]!!.addEpsilonTransition(newStates)
        else {
            val root = State<TokenScannerAlphabet, ASTCreator>()
            root.addEpsilonTransition(newStates)
            nonTerminals[name] = root
        }
    }

    fun getFSM(name: String) = FSM(nonTerminals[name] ?: State())
}

sealed class ASTData {
    class DataToken(val token: Token) : ASTData()
    class DataAST(val ast: ASTNode) : ASTData() {
        constructor(name: String, scanner: Scanner<TokenScannerAlphabet>) : this(ASTResolver(name, scanner).resolve())
    }
}

private fun Token.toASTData() = ASTData.DataToken(this)
private fun TokenScannerAlphabet.AlphabetToken.toASTData() = token.toASTData()
private fun Token.toAlphabetToken() = TokenScannerAlphabet.AlphabetToken(this)

class ASTResolver(private val fsm: FSM<TokenScannerAlphabet, ASTCreator>, private val scanner: Scanner<TokenScannerAlphabet>) {
    constructor(nonTerminal: String = "root", scanner: Scanner<TokenScannerAlphabet>) : this(NonTerminalHandler.getFSM(nonTerminal), scanner)

    private val data = mutableListOf<ASTData>()

    fun nonTerminal(name: String) {
        data.add(ASTData.DataAST(name, scanner))
    }

    fun resolve(): ASTNode {
        var node: ASTNode?
        val dataBackup = data.toList()

        do {
            data.replaceWith(dataBackup)

            fsm.reset()
            node = resolveSingular()

            if (node == null) TODO("Log Error")
        } while (node == null && scanner.isNotAtEnd())

        return node ?: NullNode
    }

    private fun resolveSingular(): ASTNode? {
        var foundCreator: ASTCreator? = null
        var foundCurrent = scanner.current + 1

        while (fsm.isRunning() && scanner.isNotAtEnd()) {
            val char = scanner.advance()
            fsm.accept(char)

            if (char is TokenScannerAlphabet.AlphabetToken) data.add(char.toASTData())

            if (fsm.isAccepting()) {
                foundCreator = fsm.getCurrentOutput().first()
                foundCurrent = scanner.current
            }
        }

        scanner.current = foundCurrent
        return foundCreator?.invoke(data.toList())
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
        machine.accept(it.toAlphabetToken())
    }

    println(machine.isAccepting())
}