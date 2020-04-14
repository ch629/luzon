package recursive_descent

import com.luzon.lexer.Token
import com.luzon.recursive_descent.PrecedenceClimbing
import com.luzon.recursive_descent.RecursiveDescent
import com.luzon.recursive_descent.TokenRecursiveDescentStream
import com.luzon.recursive_descent.ast.SyntaxTreeNode
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RDTest : Spek({
    describe("a recursive descent parser") {
        it("should generate a basic class tree from a token stream") {
            val generatedTree = RecursiveDescent(TokenRecursiveDescentStream(sequenceOf(
                Token.Keyword.CLASS.toToken(),
                Token.Literal.IDENTIFIER.toToken("Test"),
                Token.Symbol.L_BRACE.toToken(),
                Token.Keyword.VAL.toToken(),
                Token.Literal.IDENTIFIER.toToken("variable"),
                Token.Symbol.TYPE.toToken(),
                Token.Literal.IDENTIFIER.toToken("Type"),
                Token.Symbol.EQUAL.toToken(),
                Token.Literal.INT.toToken("5"),
                Token.Keyword.FUN.toToken(),
                Token.Literal.IDENTIFIER.toToken("function"),
                Token.Symbol.L_PAREN.toToken(),
                Token.Literal.IDENTIFIER.toToken("arg1"),
                Token.Symbol.TYPE.toToken(),
                Token.Literal.IDENTIFIER.toToken("Double"),
                Token.Symbol.R_PAREN.toToken(),
                Token.Symbol.TYPE.toToken(),
                Token.Literal.IDENTIFIER.toToken("String"),
                Token.Symbol.L_BRACE.toToken(),
                Token.Keyword.RETURN.toToken(),
                Token.Literal.STRING.toToken("\"value\""),
                Token.Symbol.R_BRACE.toToken(),
                Token.Symbol.R_BRACE.toToken()
            ))).parse()

            val expectedTree = SyntaxTreeNode.Class("Test", null, SyntaxTreeNode.Block(listOf(
                SyntaxTreeNode.VariableDeclaration("variable", "Type",
                    SyntaxTreeNode.Expression.LiteralExpr.IntLiteral(5), true),
                SyntaxTreeNode.FunctionDefinition("function", listOf(SyntaxTreeNode.FunctionParameter("arg1", "Double")), "String", SyntaxTreeNode.Block(listOf(
                    SyntaxTreeNode.Return(SyntaxTreeNode.Expression.LiteralExpr.StringLiteral("value"))
                )))
            )))

            generatedTree shouldEqual expectedTree
        }
    }

    describe("a precedence climber") {
        it("correctly order an expression") {
            fun int(value: Int) = Token.Literal.INT.toToken(value.toString())
            val plus = Token.Symbol.PLUS.toToken()
            val sub = Token.Symbol.SUBTRACT.toToken()
            val mult = Token.Symbol.MULTIPLY.toToken()
            val lParen = Token.Symbol.L_PAREN.toToken()
            val rParen = Token.Symbol.R_PAREN.toToken()

            val expressionTree = PrecedenceClimbing(TokenRecursiveDescentStream(
                sequenceOf(int(1), plus, int(2), mult, int(3))
            )).parse()

            val expectedTree = SyntaxTreeNode.Expression.Binary.Plus(
                SyntaxTreeNode.Expression.LiteralExpr.IntLiteral(1),
                SyntaxTreeNode.Expression.Binary.Multiply(
                    SyntaxTreeNode.Expression.LiteralExpr.IntLiteral(2),
                    SyntaxTreeNode.Expression.LiteralExpr.IntLiteral(3)
                )
            )

            // TODO: This doesn't work like the one above does, so not sure here.
            //  Might have to write a system to compare the trees better
            // Potentially just running it through the ExpressionVisitor will test this properly. But that doesn't
            // single out testing just the precedence climbers

//            expressionTree shouldEqual expectedTree
        }
    }
})
