package io.snabble.sdk.ui.widgets.toggle.di

import io.snabble.sdk.ui.widgets.toggle.repository.ToggleRepository
import io.snabble.sdk.ui.widgets.toggle.repository.ToggleRepositoryImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val toggleWidgetModule = module {
    factoryOf(::ToggleRepositoryImpl) bind ToggleRepository::class
}
