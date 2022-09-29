package io.snabble.sdk.di

import org.koin.core.Koin
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope

internal object KoinProvider : KoinScopeComponent {

    override val scope: Scope by lazy { createScope(this) }

    override fun getKoin(): Koin = koin

    private lateinit var koin: Koin

    fun setKoin(koin: Koin) {
        this.koin = koin
    }
}
