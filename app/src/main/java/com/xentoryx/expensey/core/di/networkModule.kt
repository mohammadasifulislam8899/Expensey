package com.xentoryx.expensey.core.di

import com.xentoryx.expensey.core.data.networking.HttpClientFactory
import com.xentoryx.expensey.core.storage.CurrencyConverter
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create(CIO.create()) }
    single { CurrencyConverter(get(), get()) }
}