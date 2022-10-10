package io.snabble.sdk.widgets.snabble.toggle.di

import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepositoryImpl
import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val toggleWidgetModule = module {
    factoryOf(::ToggleRepositoryImpl) bind ToggleRepository::class
}
