package com.luzon.recursive_descent

internal abstract class RecursiveParser<T>(val rd: TokenRecursiveDescentStream) {
    abstract fun parse(): T?
}
