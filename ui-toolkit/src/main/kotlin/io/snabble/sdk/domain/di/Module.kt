package io.snabble.sdk.domain.di

import io.snabble.sdk.domain.ConfigMapper
import io.snabble.sdk.domain.ConfigMapperImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val domainModule = module {
    factoryOf(::ConfigMapperImpl) bind ConfigMapper::class
}
