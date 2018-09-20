package com.luzon.parser.generator

import com.luzon.parser.ParserDSL
import com.luzon.parser.literal
import kotlin.reflect.KClass

// Primary Class (sealed class): Expr
// Sub Classes:
//   IncrementExpr
//   PlusExpr
//   SubExpr
//   NotExpr
// Accessor should exist from the Accessor definition

//region Extension Helpers

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

//endregion

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

open class ParserInterfaceFunction(val name: String, val prefix: String? = null,
                                   val parameterList: List<ParserFunctionParameter>? = null,
                                   val returnType: String? = null) {
    constructor(name: String, prefix: String? = null, vararg paramList: ParserFunctionParameter, returnType: String? = null) :
            this(name, prefix, paramList.toList(), returnType)

    override fun toString() = StringBuffer().apply {
        if (prefix != null) {
            append(prefix)
            append(" ")
        }
        append("fun ")
        append(name)
        append("(")
        if (parameterList != null) append(parameterList.joinToString(", "))
        append(")")

        if (returnType != null) {
            append(": ")
            append(returnType)
        }
    }.toString()
}

class ParserClassFunction(name: String, prefix: String? = null,
                          parameterList: List<ParserFunctionParameter>? = null,
                          returnType: String? = null,
                          val equalsValue: String? = null,
                          val block: String? = null) :
        ParserInterfaceFunction(name, prefix, parameterList, returnType) {
    override fun toString() = StringBuffer().apply {
        val indent = " ".repeat(4)
        append(super.toString())

        if (equalsValue != null) {
            append(" = ")
            append(equalsValue)
        }

        if (block != null) {
            appendln(" {")
            append(indent)
            appendln(block)
            append("}")
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

sealed class ParserClass(val name: String,
                         val parameterList: List<ParserClassParameter>? = null,
                         val superClass: String? = null,
                         val functions: List<ParserClassFunction>? = null) {
    class ParserSealedClass(name: String, parameterList: List<ParserClassParameter>? = null, superClass: String? = null) :
            ParserClass(name, parameterList, superClass) {
        override val prefix: String
            get() = "sealed"

        val subClasses = mutableListOf<ParserClass>()

        private fun createSubClass(clazz: ParserClass) {
            subClasses.add(clazz)
        }

        fun createSubClass(name: String, parameterList: List<ParserClassParameter>) =
                ParserNormalClass(name, parameterList, this.name).apply {
                    createSubClass(this)
                }

        fun createSubDataClass(name: String, parameterList: List<ParserClassParameter>, functions: List<ParserClassFunction>? = null) =
                ParserDataClass(name, parameterList, "${this.name}()", functions).apply {
                    createSubClass(this)
                }

        fun createSubClass(name: String, vararg parameters: ParserClassParameter) =
                createSubClass(name, parameters.toList())

        fun createSubDataClass(name: String, vararg parameters: ParserClassParameter) =
                createSubDataClass(name, parameters.toList())

        override fun toString() = StringBuffer().apply {
            append(super.toString())
            if (subClasses.isEmpty()) return@apply
            val indent = " ".repeat(4)
            append(" ")
            appendln("{")

            append(indent)
            append(subClasses.joinToString("\n$indent"))

//            subClasses.forEach {
//                append(indent)
//                appendln(it.toString())
//            }

            appendln("}")
        }.toString()
    }

    class ParserDataClass(name: String, parameterList: List<ParserClassParameter>, superClass: String? = null, functions: List<ParserClassFunction>? = null) :
            ParserClass(name, parameterList, superClass, functions) {
        override val prefix: String
            get() = "data"
    }

    class ParserNormalClass(name: String, parameterList: List<ParserClassParameter>, superClass: String? = null, functions: List<ParserClassFunction>? = null) :
            ParserClass(name, parameterList, superClass, functions)

    open val prefix: String? = null

    override fun toString() = StringBuffer().apply {
        if (prefix != null) {
            append(prefix)
            append(" ")
        }

        append("class ")
        append(name)

        if (parameterList != null) {
            append("(")
            append(parameterList.joinToString(", "))
            append(")")
        }

        if (superClass != null) {
            append(" : ")
            append(superClass)
        }

        if (functions != null) {
            val indent = " ".repeat(4)
            appendln(" {")

            functions.forEach {
                append(indent.repeat(2))
                appendln(it.toString())
            }

            append(indent)
            appendln("}")
        }
    }.toString()


    fun asParameter(name: String, defaultValue: String? = null) =
            ParserClassParameter(name, this.name, defaultValue)

    fun asNullableParameter(name: String, defaultValue: String? = null) =
            ParserClassParameter(name, "${this.name}?", defaultValue)
}

data class ParserClassParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = "val $name: $type${if (defaultValue != null) " = $defaultValue" else ""}"
}

fun generateVisitorFromSealedClass(clazz: ParserClass.ParserSealedClass) =
        ParserInterface("${clazz.name}Visitor<T>", clazz.subClasses.map {
            ParserInterfaceFunction("visit", null,
                    ParserFunctionParameter(clazz.name.decapitalize(), "${clazz.name}.${it.name}"), returnType = "T")
        })

data class TrioClass(val sealedClass: ParserClass.ParserSealedClass, val visitorInterface: ParserInterface, val visitableInterface: ParserInterface) {
    override fun toString() = StringBuffer().apply {
        appendln(visitorInterface.toString())
        appendln(visitableInterface.toString())
        append(sealedClass.toString())
    }.toString()
}

fun generateClassWithVisitor(parserDSL: ParserDSL): TrioClass =
        generateClassWithVisitor(parserDSL.toParserGeneratorClass())

fun generateClassWithVisitor(clazz: ParserClass.ParserSealedClass): TrioClass {
    val visitorInterface = generateVisitorFromSealedClass(clazz)
    val visitableInterface = ParserInterface("${clazz.name}Visitable",
            listOf(ParserInterfaceFunction("<T> accept", null,
                    listOf(ParserFunctionParameter("visitor", "${clazz.name}Visitor<T>")), "T")
            ))

    return TrioClass(clazz, visitorInterface, visitableInterface)
}

fun main(args: Array<String>) {
    println(generateClassWithVisitor(literal))
}
