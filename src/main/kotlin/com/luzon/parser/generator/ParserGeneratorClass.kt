package com.luzon.parser.generator

import com.luzon.parser.ParserDSL
import com.luzon.parser.expr
import okio.buffer
import okio.sink
import java.io.File

private val indent = " ".repeat(4) // 4 Spaces

class Interface(val name: String, val typeParameter: String = "", val functions: List<Function>) {
    override fun toString() = StringBuffer().apply {
        append("interface $name")

        if (typeParameter.isNotBlank())
            append("<$typeParameter>")

        appendln(" {")

        append(indent)
        appendln(functions.joinToString("\n").indentNewLines())

        appendln("}")
    }.toString()
}

class SealedClass(val name: String,
                  private val superClass: String,
                  var innerClasses: List<DataClass> = emptyList()) {

    override fun toString() = StringBuffer().apply {
        appendln("sealed class $name : $superClass {")

        append(indent)
        appendln(innerClasses.joinToString("\n\n").indentNewLines())

        appendln("}")
    }.toString()

    fun createSubClass(name: String, visitorName: String, parameters: List<ClassParameter>): DataClass {
        val dataClass = DataClass(
                name = name,
                parameters = parameters,
                superClass = this.name,
                functions = listOf(dataClassFunction(visitorName))
        )

        innerClasses += dataClass
        return dataClass
    }
}

class DataClass(val name: String,
                private val parameters: List<ClassParameter>,
                private val superClass: String,
                private val functions: List<Function>) {
    override fun toString() = StringBuffer().apply {
        appendln("data class $name(${parameters.joinToString(", ")}) : $superClass() {")

        append(indent)
        appendln(functions.joinToString("\n").indentNewLines())

        append("}")
    }.toString()
}

data class Function(val prefix: String = "",
                    val typeParameter: String = "",
                    val name: String,
                    val parameter: FunctionParameter,
                    val returnType: String = "",
                    val equals: String = "") {
    override fun toString() = StringBuffer().apply {
        if (prefix.isNotBlank())
            append("$prefix ")

        append("fun ")

        if (typeParameter.isNotBlank())
            append("<$typeParameter> ")

        append("$name($parameter)")

        if (returnType.isNotBlank())
            append(": $returnType")

        if (equals.isNotBlank())
            append(" = $equals")
    }.toString()
}

open class FunctionParameter(private val name: String, private val type: String) {
    override fun toString() = "$name: $type"
}

class ClassParameter(name: String, type: String) : FunctionParameter(name, type) {
    override fun toString() = "val ${super.toString()}"
}

private fun visitorFunction(typeName: String, type: String) = Function(
        name = "visit",
        parameter = FunctionParameter(typeName, type),
        returnType = "T"
)

private fun visitableFunction(type: String) = Function(
        typeParameter = "T",
        name = "accept",
        parameter = FunctionParameter("visitor", "$type<T>"),
        returnType = "T"
)

private fun dataClassFunction(typeName: String) = Function(
        prefix = "override",
        typeParameter = "T",
        name = "accept",
        parameter = FunctionParameter("visitor", "$typeName<T>"),
        equals = "visitor.visit(this)"
)

private fun visitorInterface(name: String, parameterName: String, parameterTypes: List<String>) = Interface(
        name = "${name}Visitor",
        typeParameter = "T",
        functions = parameterTypes.map { visitorFunction(parameterName, it) }
)

private fun visitableInterface(name: String, visitorType: String) = Interface(
        name = "${name}Visitable",
        functions = listOf(visitableFunction("${visitorType}Visitor"))
)

private fun String.indentNewLines() = replace("\n", "\n$indent")

data class TrioClass(val sealedClass: SealedClass, val visitorInterface: Interface,
                     val visitableInterface: Interface) {
    override fun toString() = StringBuffer().apply {
        appendln(visitorInterface.toString())
        appendln(visitableInterface.toString())
        append(sealedClass.toString())
    }.toString()

    fun toFile(path: String) {
        val file = File(path)
        if (file.exists())
            file.delete()

        file.parentFile.mkdirs()
        file.createNewFile()

        val bufferedSink = file.sink().buffer()
        bufferedSink.writeUtf8(this.toString())
        bufferedSink.flush()
    }
}

private fun generateVisitorFromSealedClass(clazz: SealedClass) = visitorInterface(
        name = clazz.name,
        parameterName = clazz.name.decapitalize(),
        parameterTypes = clazz.innerClasses.map { "${clazz.name}.${it.name}" }
)

private fun generateVisitableFromSealedClass(clazz: SealedClass) = visitableInterface(clazz.name, clazz.name)

fun generateClassWithVisitor(parserDSL: ParserDSL): TrioClass =
        generateClassWithVisitor(parserDSL.toNewParserGeneratorClass())

fun generateClassWithVisitor(clazz: SealedClass): TrioClass {
    val visitorInterface = generateVisitorFromSealedClass(clazz)
    val visitableInterface = generateVisitableFromSealedClass(clazz)

    return TrioClass(clazz, visitorInterface, visitableInterface)
}

fun main(args: Array<String>) {
    println(generateClassWithVisitor(expr))
}