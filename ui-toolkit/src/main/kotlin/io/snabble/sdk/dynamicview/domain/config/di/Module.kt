package io.snabble.sdk.dynamicview.domain.config.di

import android.content.res.AssetManager
import io.snabble.sdk.Snabble
import io.snabble.sdk.dynamicview.domain.config.ConfigFileProvider
import io.snabble.sdk.dynamicview.domain.config.ConfigFileProviderImpl
import io.snabble.sdk.dynamicview.domain.config.ConfigMapper
import io.snabble.sdk.dynamicview.domain.config.ConfigMapperImpl
import io.snabble.sdk.dynamicview.domain.config.ConfigRepository
import io.snabble.sdk.dynamicview.domain.config.ConfigRepositoryImpl
import io.snabble.sdk.dynamicview.domain.config.SsidProvider
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val configModule = module {
    single { Json { ignoreUnknownKeys = true } }

    factory { androidContext().resources.assets } bind AssetManager::class

    factoryOf(::ConfigMapperImpl) bind ConfigMapper::class

    factory<SsidProvider> { SsidProvider { Snabble.currentCheckedInShop.value?.customerNetworks?.firstOrNull()?.ssid } }

    factory {
        ConfigFileProviderImpl(
            assetManager = get(),
        )
    } bind ConfigFileProvider::class

    factory {
        ConfigRepositoryImpl(
            fileProvider = get(),
            json = get(),
            configMapper = get(),
        )
    } bind ConfigRepository::class
}
