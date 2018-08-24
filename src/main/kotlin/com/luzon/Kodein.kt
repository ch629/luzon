package com.luzon

import com.luzon.lexer.TokenMachine
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

internal val kodein = Kodein {
    bind<TokenMachine>() with singleton { TokenMachine }
}

