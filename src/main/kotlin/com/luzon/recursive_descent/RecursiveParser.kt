package com.luzon.recursive_descent

internal abstract class RecursiveParser<T>(val rd: TokenRDStream) {
    abstract fun parse(): T?
}
