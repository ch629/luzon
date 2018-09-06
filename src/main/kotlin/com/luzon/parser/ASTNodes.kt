package com.luzon.parser

import com.luzon.fsm.FSM
import com.luzon.fsm.State
import com.luzon.lexer.Token
import com.luzon.lexer.Token.*

interface ASTNode

interface Statement : ASTNode
interface ClassStatement : Statement
interface Expression : ASTNode

object NullNode : ASTNode

data class ClassBlock(val statements: List<ClassStatement>) : ASTNode
data class FunctionBlock(val statement: List<Statement>) : ASTNode
data class Block(val statements: List<Statement>) : ASTNode

data class Argument(val expression: Expression) : ASTNode
data class FunctionCall(val name: String, val arguments: List<Argument>) : Statement, Expression
data class Return(val expression: Expression) : Statement

private fun ASTData.tokenData() = (this as ASTData.DataToken).token.data
private fun ASTData.ast() = (this as ASTData.DataAST).ast

private fun consistsOf(vararg tokens: TokenEnum) {
    TODO()
}

class ASTDSL {
    //TODO: Other ASTNodes
    private var currentState = State<TokenEnum, Boolean>()
    private val root = State<TokenEnum, Boolean>().addEpsilonTransition(currentState)
    private var followedByPointer: State<TokenEnum, Boolean>? = null

    operator fun TokenEnum.unaryPlus() {
        val newState = State<TokenEnum, Boolean>()
        currentState.addTransition({ it == this }, newState)
        currentState = newState

    }

    operator fun String.unaryPlus() {
        TODO("Add Non-terminal to be expanded")
    }

    //TODO: I may need to use both String & ASTNode, as the String version can be resolved at a later time (If it hasn't been created yet)
    operator fun ASTNode.unaryPlus() {
        TODO("Or use this as the non-terminal?")
    }

    val or get() = or()
    val self get() = self()

    fun self() {
        TODO("Adds a non-terminal of itself -> Needed within expr")
    }

    fun or() {

    }

    fun or(vararg tokens: TokenEnum) {
        val newState = State<TokenEnum, Boolean>()
        tokens.forEach { token -> currentState.addTransition({ it == token }, newState) }
        currentState = newState
    }

    fun toASTNode(): ASTNode {
        TODO()
    }

    fun toFSM() = FSM(root)

    //Groups the next part after this current section (val | var) (identifier type identifier | identifier equal <expr> | identifier type identifier equal <expr>)
    fun followedBy(block: ASTDSL.() -> Unit) {
        val dsl = ASTDSL().apply(block)
        currentState.addEpsilonTransition(dsl.root)
    }
}

private fun consistsOf(block: ASTDSL.() -> Unit) = ASTDSL().apply(block).toASTNode()

var accessor = consistsOf {
    +Literal.IDENTIFIER
    or
    +"literal"
    or
    +"fun_call"
    or
    self
    +Symbol.DOT
    self
    or
    self
    +"array_access"
}

var expr = consistsOf {
    +accessor
    or
    or(Symbol.SUBTRACT, Symbol.NOT)
    self
    or
    +Symbol.L_PAREN
    self
    +Symbol.R_PAREN
    or
    self
    or(Symbol.PLUS, Symbol.SUBTRACT, Symbol.MULTIPLY,
            Symbol.DIVIDE, Symbol.MOD, Symbol.GREATER,
            Symbol.LESS, Symbol.GREATER_EQUAL, Symbol.LESS_EQUAL,
            Symbol.EQUAL_EQUAL, Symbol.NOT_EQUAL, Symbol.OR,
            Symbol.AND, Symbol.PLUS_ASSIGN, Symbol.SUBTRACT_ASSIGN,
            Symbol.MULTIPLY_ASSIGN, Symbol.DIVIDE_ASSIGN, Symbol.MOD_ASSIGN)
    self
    or
    self
    or(Symbol.INCREMENT, Symbol.DECREMENT)
    or
    or(Symbol.INCREMENT, Symbol.DECREMENT)
    self
}

//TODO: Try and figure something out with this
//TODO: I may need to generate classes using these (Parser Generator)
var var_decl = consistsOf {
    or(Keyword.VAL, Keyword.VAR)

    followedBy {
        +Literal.IDENTIFIER
        +Symbol.TYPE
        +Literal.IDENTIFIER
        or
        +Literal.IDENTIFIER
        +Symbol.EQUAL
        +expr
        or
        +Literal.IDENTIFIER
        +Symbol.TYPE
        +Literal.IDENTIFIER
        +Symbol.EQUAL
        +expr
    }
}

val variableDeclarationCreator: ASTCreator = {
    val name: String = it[0].tokenData()
    var type: String? = null
    var expression: Expression? = null

    if (it[2] is ASTData.DataToken)
        type = it[2].tokenData()

    if (it[2] is ASTData.DataAST)
        expression = it[2].ast() as Expression

    if (it.size == 3)
        expression = it[3].ast() as Expression

    if (type != null && expression != null) VariableDeclaration(name, type, expression)
    if (type != null) VariableDeclaration(name, type)
    if (expression != null) VariableDeclaration(name, expression)

    NullNode
}

class VariableDeclaration : Statement {
    val name: String
    val type: String?
    val assign: Expression?

    constructor(name: String, type: String) {
        this.name = name
        this.type = type
        this.assign = null
    }

    constructor(name: String, assign: Expression) {
        this.name = name
        this.type = null
        this.assign = assign
    }

    constructor(name: String, type: String, assign: Expression) {
        this.name = name
        this.type = type
        this.assign = assign
    }
}

data class Parameter(val name: String, val type: String) : ASTNode
data class FunctionDeclaration(val name: String, val parameters: List<Parameter>, val block: FunctionBlock) : ClassStatement //TODO: Default parameter values?

data class Class(val name: String, val block: ClassBlock) : ASTNode //TODO: Constructors, Inheritance

data class IfStatement(val expression: Expression, val block: Block, val elseStatement: Else?) : Statement
data class Else(val block: Block) : ASTNode

data class BinaryExpression(val operator: Token.Symbol, val op1: Expression, val op2: Expression) : Expression
data class UnaryExpression(val operator: Token.Symbol, val op: Expression) : Expression
data class LiteralExpression(val literal: Token) : Expression

data class VariableAccess(val name: String) : Expression

enum class VariableDeclarationType {
    VAR, VAL
}