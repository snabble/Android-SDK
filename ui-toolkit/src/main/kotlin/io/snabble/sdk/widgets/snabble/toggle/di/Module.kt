package io.snabble.sdk.widgets.snabble.toggle.di

import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository
import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepositoryImpl
import io.snabble.sdk.widgets.snabble.toggle.usecases.GetToggleStateUseCase
import io.snabble.sdk.widgets.snabble.toggle.usecases.GetToggleStateUseCaseImpl
import io.snabble.sdk.widgets.snabble.toggle.usecases.SaveToggleStateUseCase
import io.snabble.sdk.widgets.snabble.toggle.usecases.SaveToggleStateUseCaseImpl
import io.snabble.sdk.widgets.snabble.toggle.viewmodel.ToggleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val toggleWidgetModule = module {
    factoryOf(::ToggleRepositoryImpl) bind ToggleRepository::class

    factory { params ->
        GetToggleStateUseCaseImpl(
            prefKey = params[0],
            toggleRepository = get(),
        )
    } bind GetToggleStateUseCase::class

    factory { params ->
        SaveToggleStateUseCaseImpl(
            prefKey = params[0],
            toggleRepository = get(),
        )
    } bind SaveToggleStateUseCase::class

    viewModel { params -> ToggleViewModel(get { params }, get { params }) }
}
