package com.luzon.rd

internal abstract class RecursiveParser<T>(val rd: TokenRDStream) {
    abstract fun parse(): T?
}