package io.snabble.sdk.di

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

internal object KoinProvider : KoinComponent {

    override fun getKoin(): Koin = koin

    private lateinit var koin: Koin

    fun setKoin(koin: Koin) {
        this.koin = koin
    }
}
