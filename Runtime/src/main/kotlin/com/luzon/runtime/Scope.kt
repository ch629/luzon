package com.luzon.runtime

class Scope {
    private val parent: Scope? = null
    private val symbols: HashMap<String, TerminalSymbol> = hashMapOf()

    fun findSymbol(name: String): TerminalSymbol? = when {
        symbols.containsKey(name) -> symbols[name]!!
        parent != null -> parent.findSymbol(name)
        else -> null
    }
}