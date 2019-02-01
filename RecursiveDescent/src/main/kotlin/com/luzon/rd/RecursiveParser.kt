package com.luzon.rd

internal abstract class RecursiveParser<T>(val rd: RecursiveDescent) {
    abstract fun parse(): T?
}