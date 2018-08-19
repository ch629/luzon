package com.luzon

import com.luzon.lexer.TokenRegexJson
import com.squareup.moshi.Moshi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

internal val kodein = Kodein {
    bind<Moshi>() with singleton { Moshi.Builder().build() }
    bind<TokenRegexJson>() with singleton { TokenRegexJson.fromJson() }
}

