package com.luzon.parser.generator

import com.luzon.parser.ParserDSL
import com.luzon.parser.literal

private val indent = " ".repeat(4)

class ParserInterface(val name: String,
                      val interfaceFunctions: MutableList<ParserInterfaceFunction> = mutableListOf(),
                      val superClass: String? = null) {
    override fun toString() = StringBuffer().apply {
        val indent = " ".repeat(4)
        append("interface $name")

        if (superClass != null)
            append(" : $superClass")

        appendln(" {")

        interfaceFunctions.forEach {
            appendln("$indent$it")
        }

        appendln("}")
    }.toString()

    fun addFunction(function: ParserInterfaceFunction) {
        interfaceFunctions.add(function)
    }

    fun addFunction(name: String, prefix: String? = null,
                    parameterList: List<ParserFunctionParameter>? = null,
                    returnType: String? = null) {
        addFunction(ParserInterfaceFunction(name, prefix, parameterList, returnType))
    }


    fun addFunction(name: String, prefix: String? = null,
                    parameterList: ParserFunctionParameter,
                    returnType: String? = null) {
        addFunction(name, prefix, listOf(parameterList), returnType)
    }
}

open class ParserInterfaceFunction(val name: String, val prefix: String? = null,
                                   val parameterList: List<ParserFunctionParameter>? = null,
                                   val returnType: String? = null) {
    constructor(name: String, prefix: String? = null, vararg paramList: ParserFunctionParameter, returnType: String? = null) :
            this(name, prefix, paramList.toList(), returnType)

    override fun toString() = StringBuffer().apply {
        if (prefix != null)
            append("$prefix ")

        append("fun $name(")
        if (parameterList != null) append(parameterList.joinToString(", "))
        append(")")

        if (returnType != null)
            append(": $returnType")
    }.toString()
}

class ParserClassFunction(name: String, prefix: String? = null,
                          parameterList: List<ParserFunctionParameter>? = null,
                          returnType: String? = null,
                          val equalsValue: String? = null,
                          val block: String? = null) :
        ParserInterfaceFunction(name, prefix, parameterList, returnType) {
    override fun toString() = StringBuffer().apply {
        append(super.toString())

        if (equalsValue != null)
            append(" = $equalsValue")

        if (block != null) {
            appendln(" {")
            appendln("$indent$block")
            append("}")
        }
    }.toString()
}

class ParserFunctionParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = StringBuffer().apply {
        append("$name: $type")

        if (defaultValue != null)
            append(" = $defaultValue")
    }.toString()
}

sealed class ParserClass(val name: String,
                         val parameterList: List<ParserClassParameter>? = null,
                         val superClass: String? = null,
                         val functions: MutableList<ParserClassFunction> = mutableListOf()) {
    class ParserSealedClass(name: String, parameterList: List<ParserClassParameter>? = null,
                            superClass: String? = null) :
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

        fun createSubDataClass(name: String, parameterList: List<ParserClassParameter>,
                               functions: MutableList<ParserClassFunction> = mutableListOf()) =
                ParserDataClass(name, parameterList, "${this.name}()", functions).apply {
                    createSubClass(this)
                }

        override fun toString() = StringBuffer().apply {
            append(super.toString())
            if (subClasses.isEmpty()) return@apply
            appendln(" {")

            append("$indent${subClasses.joinToString("\n$indent")}")

            appendln("}")
        }.toString()
    }

    class ParserDataClass(name: String, parameterList: List<ParserClassParameter>, superClass: String? = null,
                          functions: MutableList<ParserClassFunction> = mutableListOf()) :
            ParserClass(name, parameterList, superClass, functions) {
        override val prefix: String
            get() = "data"
    }

    class ParserNormalClass(name: String, parameterList: List<ParserClassParameter>, superClass: String? = null,
                            functions: MutableList<ParserClassFunction> = mutableListOf()) :
            ParserClass(name, parameterList, superClass, functions)

    open val prefix: String? = null

    override fun toString() = StringBuffer().apply {
        if (prefix != null)
            append("$prefix ")

        append("class $name")

        if (parameterList != null && parameterList.isNotEmpty())
            append("(${parameterList.joinToString(", ")})")

        if (superClass != null)
            append(" : $superClass")

        if (functions.isNotEmpty()) {
            appendln(" {")

            functions.forEach {
                append(indent.repeat(2))
                appendln(it.toString())
            }

            append(indent)
            appendln("}")
        }
    }.toString()

    fun addFunction(function: ParserClassFunction) {
        functions.add(function)
    }

    fun addFunction(name: String, prefix: String? = null,
                    parameterList: List<ParserFunctionParameter>? = null,
                    returnType: String? = null,
                    equalsValue: String? = null,
                    block: String? = null) =
            ParserClassFunction(name, prefix, parameterList, returnType, equalsValue, block).apply {
                addFunction(this)
            }

}

data class ParserClassParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = StringBuffer().apply {
        append("val $name: $type")

        if (defaultValue != null)
            append(" = $defaultValue")
    }.toString()
}

fun generateVisitorFromSealedClass(clazz: ParserClass.ParserSealedClass) =
        ParserInterface("${clazz.name}Visitor<T>").apply {
            clazz.subClasses.forEach {
                val param = ParserFunctionParameter(clazz.name.decapitalize(), "${clazz.name}.${it.name}")
                addFunction("visit", null, param, returnType = "T")
            }
        }

data class TrioClass(val sealedClass: ParserClass.ParserSealedClass, val visitorInterface: ParserInterface,
                     val visitableInterface: ParserInterface) {
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
    val visitableInterface = ParserInterface("${clazz.name}Visitable").apply {
        val param = ParserFunctionParameter("visitor", "${clazz.name}Visitor<T>")
        addFunction("<T> accept", null, param, "T")
    }

    return TrioClass(clazz, visitorInterface, visitableInterface)
}

fun main(args: Array<String>) {
    println(generateClassWithVisitor(literal))
}
