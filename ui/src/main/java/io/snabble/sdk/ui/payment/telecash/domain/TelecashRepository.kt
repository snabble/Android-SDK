package io.snabble.sdk.ui.payment.telecash.domain

import java.net.URL

interface TelecashRepository {

    fun preAuth(userDetails: UserDetails): URL
}
