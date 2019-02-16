package com.luzon.runtime

class Scope private constructor(private val parent: Scope?) { // TODO: If null, then global
    private val symbols: HashMap<String, TerminalSymbol> = hashMapOf()

    companion object {
        val globalScope = Scope(null)
    }

    fun findSymbol(name: String): TerminalSymbol? = when {
        symbols.containsKey(name) -> symbols[name]!!
        parent != null -> parent.findSymbol(name)
        else -> null
    }

    fun newScope(): Scope = Scope(this)

    fun addSymbol(name: String, symbol: TerminalSymbol) {
        symbols[name] = symbol
    }
}