package io.snabble.sdk.ui.payment.telecash.data

import io.snabble.sdk.ui.payment.telecash.domain.TelecashRepository
import io.snabble.sdk.ui.payment.telecash.domain.UserDetails
import java.net.URL

class TelecashRepositoryImpl : TelecashRepository {

    override fun preAuth(userDetails: UserDetails): URL {
        TODO("Not yet implemented")
    }
}
