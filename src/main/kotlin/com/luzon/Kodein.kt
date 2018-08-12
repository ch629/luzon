package com.luzon

import com.squareup.moshi.Moshi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

val kodein = Kodein {
    bind<Moshi>() with singleton { Moshi.Builder().build() }
}

