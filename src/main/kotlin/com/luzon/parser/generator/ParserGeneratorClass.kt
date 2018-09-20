package com.luzon.parser.generator

import com.luzon.parser.literal
import kotlin.reflect.KClass

// Primary Class (sealed class): Expr
// Sub Classes:
//   IncrementExpr
//   PlusExpr
//   SubExpr
//   NotExpr
// Accessor should exist from the Accessor definition

private fun KClass<*>.asParameter(name: String, defaultValue: String? = null) =
        ParserClassParameter(name, simpleName!!, defaultValue)

private fun Any.asParameter(name: String, defaultValue: String? = null): ParserClassParameter =
        ParserClassParameter(name, this::class.simpleName!!, defaultValue)

private fun Any.asDefaultParameter(name: String) =
        ParserClassParameter(name, this::class.simpleName!!, toString())

private fun KClass<*>.asNullableParameter(name: String, defaultValue: String? = null) =
        ParserClassParameter(name, "${simpleName!!}?", defaultValue)

private fun Any.asNullableParameter(name: String, defaultValue: String? = null): ParserClassParameter =
        ParserClassParameter(name, "${this::class.simpleName!!}?", defaultValue)

private fun Any.asNullableDefaultParameter(name: String) =
        ParserClassParameter(name, "${this::class.simpleName!!}?", toString())

class ParserInterface(val name: String, val interfaceFunctions: List<ParserInterfaceFunction>, val superClass: String? = null) {
    override fun toString() = StringBuffer().apply {
        val indent = " ".repeat(4)
        append("interface ")
        append(name)

        if (superClass != null) {
            append(" : ")
            append(superClass)
        }

        appendln(" {")

        interfaceFunctions.forEach {
            append(indent)
            appendln(it.toString())
        }

        appendln("}")
    }.toString()
}

class ParserInterfaceFunction(val name: String, val parameterList: ParserFunctionParameterList? = null, val returnType: String? = null) {
    constructor(name: String, vararg paramList: ParserFunctionParameter, returnType: String? = null) :
            this(name, ParserFunctionParameterList(*paramList), returnType)

    override fun toString() = StringBuffer().apply {
        append("fun ")
        append(name)
        append("(")
        if (parameterList != null)
            append(parameterList.toString())
        append(")")

        if (returnType != null) {
            append(": ")
            append(returnType)
        }
    }.toString()
}

class ParserFunctionParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = StringBuffer().apply {
        append(name)
        append(": ")
        append(type)

        if (defaultValue != null) {
            append(" = ")
            append(defaultValue)
        }
    }.toString()
}

class ParserFunctionParameterList(val parameters: List<ParserFunctionParameter>) {
    constructor(vararg params: ParserFunctionParameter) : this(params.toList())

    override fun toString() = parameters.joinToString(", ")
}

sealed class ParserClass(val name: String, val parameterList: ParserParameterList? = null, val superClass: String? = null) {
    class ParserSealedClass(name: String, parameterList: ParserParameterList? = null, superClass: String? = null) :
            ParserClass(name, parameterList, superClass) {
        override val prefix: String
            get() = "sealed"

        val subClasses = mutableListOf<ParserClass>()

        private fun createSubClass(clazz: ParserClass) {
            subClasses.add(clazz)
        }

        fun createSubClass(name: String, parameterList: ParserParameterList) =
                ParserNormalClass(name, parameterList, this.name).apply {
                    createSubClass(this)
                }

        fun createSubDataClass(name: String, parameterList: ParserParameterList) =
                ParserDataClass(name, parameterList, this.name).apply {
                    createSubClass(this)
                }

        fun createSubClass(name: String, vararg parameters: ParserClassParameter) =
                createSubClass(name, ParserParameterList(parameters.toList()))

        fun createSubDataClass(name: String, vararg parameters: ParserClassParameter) =
                createSubDataClass(name, ParserParameterList(parameters.toList()))

        override fun toString() = StringBuffer().apply {
            append(super.toString())
            if (subClasses.isEmpty()) return@apply
            val indent = " ".repeat(4)
            append(" ")
            appendln("{")
            subClasses.forEach {
                append(indent)
                appendln(it.toString())
            }
            appendln("}")
        }.toString()
    }

    class ParserDataClass(name: String, parameterList: ParserParameterList, superClass: String? = null) :
            ParserClass(name, parameterList, superClass) {
        override val prefix: String
            get() = "data"
    }

    class ParserNormalClass(name: String, parameterList: ParserParameterList, superClass: String? = null) :
            ParserClass(name, parameterList, superClass)

    open val prefix: String? = null

    override fun toString() =
            "${if (prefix != null) "$prefix " else ""}class $name${if (parameterList != null) "($parameterList)" else ""}${if (superClass != null) " : $superClass()" else ""}"

    fun asParameter(name: String, defaultValue: String? = null) =
            ParserClassParameter(name, this.name, defaultValue)

    fun asNullableParameter(name: String, defaultValue: String? = null) =
            ParserClassParameter(name, "${this.name}?", defaultValue)
}

data class ParserParameterList(val parameters: List<ParserClassParameter>) {
    constructor(vararg params: ParserClassParameter) : this(params.toList())

    override fun toString() = parameters.joinToString(", ")
}

data class ParserClassParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = "val $name: $type${if (defaultValue != null) " = $defaultValue" else ""}"
}

fun generateVisitorFromSealedClass(clazz: ParserClass.ParserSealedClass) =
        ParserInterface("${clazz.name}Visitor", clazz.subClasses.map {
            ParserInterfaceFunction("visit",
                    ParserFunctionParameter(clazz.name.decapitalize(), it.name))
        })

fun main(args: Array<String>) {
    val exprClass = literal.toParserGeneratorClass()
    println(exprClass)
    println(generateVisitorFromSealedClass(exprClass))
}