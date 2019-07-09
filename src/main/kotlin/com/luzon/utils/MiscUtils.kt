package com.luzon.utils

fun StringBuilder.indent(times: Int = 1): StringBuilder = append("    ".repeat(times))
