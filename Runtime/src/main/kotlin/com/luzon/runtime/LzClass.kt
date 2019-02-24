package com.luzon.runtime

import com.luzon.rd.ast.ASTNode

// TODO: Default constructor or null?
data class LzClass(val name: String, val constructor: LzFunction?, val functions: List<LzFunction>, val block: ASTNode.Block)