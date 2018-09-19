package com.luzon.parser.generator

import kotlin.reflect.KClass

// Primary Class (sealed class): Expr
// Sub Classes:
//   IncrementExpr
//   PlusExpr
//   SubExpr
//   NotExpr
// Accessor should exist from the Accessor definition

private fun KClass<*>.asParameter(name: String, defaultValue: String? = null) =
        ParserParameter(name, simpleName!!, defaultValue)

private fun Any.asParameter(name: String, defaultValue: String? = null): ParserParameter =
        ParserParameter(name, this::class.simpleName!!, defaultValue)

private fun Any.asDefaultParameter(name: String) =
        ParserParameter(name, this::class.simpleName!!, toString())

private fun KClass<*>.asNullableParameter(name: String, defaultValue: String? = null) =
        ParserParameter(name, "${simpleName!!}?", defaultValue)

private fun Any.asNullableParameter(name: String, defaultValue: String? = null): ParserParameter =
        ParserParameter(name, "${this::class.simpleName!!}?", defaultValue)

private fun Any.asNullableDefaultParameter(name: String) =
        ParserParameter(name, "${this::class.simpleName!!}?", toString())

sealed class ParserClass(val name: String, val parameterList: ParserParameterList? = null, val superClass: String? = null) {
    class ParserSealedClass(name: String, parameterList: ParserParameterList? = null, superClass: String? = null) :
            ParserClass(name, parameterList, superClass) {
        override val prefix: String
            get() = "sealed"

        private val subClasses = mutableListOf<ParserClass>()

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

        fun createSubClass(name: String, vararg parameters: ParserParameter) =
                createSubClass(name, ParserParameterList(parameters.toList()))

        fun createSubDataClass(name: String, vararg parameters: ParserParameter) =
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
            "${if (prefix != null) "$prefix " else ""}class $name${if (parameterList != null) "($parameterList)" else ""}${if (superClass != null) ": $superClass()" else ""}"

    fun asParameter(name: String, defaultValue: String? = null) =
            ParserParameter(name, this.name, defaultValue)

    fun asNullableParameter(name: String, defaultValue: String? = null) =
            ParserParameter(name, "${this.name}?", defaultValue)
}

data class ParserParameterList(val parameters: List<ParserParameter>) {
    constructor(vararg params: ParserParameter) : this(params.toList())

    override fun toString() = parameters.joinToString(", ")
}

data class ParserParameter(val name: String, val type: String, val defaultValue: String? = null) {
    override fun toString() = "val $name: $type${if (defaultValue != null) " = $defaultValue" else ""}"
}