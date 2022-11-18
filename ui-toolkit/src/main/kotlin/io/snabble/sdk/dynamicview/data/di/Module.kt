package io.snabble.sdk.dynamicview.data.di

import android.content.res.AssetManager
import io.snabble.sdk.Snabble
import io.snabble.sdk.dynamicview.data.dto.SsidProvider
import io.snabble.sdk.dynamicview.data.dto.mapper.ConfigMapper
import io.snabble.sdk.dynamicview.data.dto.mapper.ConfigMapperImpl
import io.snabble.sdk.dynamicview.data.local.ConfigFileProvider
import io.snabble.sdk.dynamicview.data.local.ConfigFileProviderImpl
import io.snabble.sdk.dynamicview.data.local.ConfigurationLocalDataSource
import io.snabble.sdk.dynamicview.data.local.ConfigurationLocalDataSourceImpl
import io.snabble.sdk.dynamicview.data.repository.ConfigRepositoryImpl
import io.snabble.sdk.dynamicview.domain.repository.ConfigRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val configModule = module {
    factoryOf(::ConfigRepositoryImpl) bind ConfigRepository::class

    factory {
        ConfigurationLocalDataSourceImpl(
            fileProvider = get(),
            json = get(),
            configMapper = get(),
        )
    } bind ConfigurationLocalDataSource::class

    single { Json { ignoreUnknownKeys = true } }

    factory { androidContext().resources.assets } bind AssetManager::class

    factory<SsidProvider> {
        SsidProvider { get<Snabble>().currentCheckedInShop.value?.customerNetworks?.firstOrNull()?.ssid }
    }

    factoryOf(::ConfigMapperImpl) bind ConfigMapper::class

    factory {
        ConfigFileProviderImpl(
            assetManager = get(),
        )
    } bind ConfigFileProvider::class
}
