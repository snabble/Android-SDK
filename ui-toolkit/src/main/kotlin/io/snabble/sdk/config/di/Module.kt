package io.snabble.sdk.config.di

import io.snabble.sdk.config.ConfigFileProvider
import io.snabble.sdk.config.ConfigFileProviderImpl
import io.snabble.sdk.config.ConfigRepository
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val configModule = module {
    single { Json { ignoreUnknownKeys = true } }
    factoryOf(::ConfigFileProviderImpl) bind ConfigFileProvider::class
    factoryOf(::ConfigRepository)
}
