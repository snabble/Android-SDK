package io.snabble.sdk.ui.payment.telecash.data

import java.net.URL

interface TelecashRemoteDataSource {

    suspend fun preAuth(userDetails: UserDetailsDto): URL
}

class TelecashRemoteDataSourceImpl : TelecashRemoteDataSource {

    override suspend fun preAuth(userDetails: UserDetailsDto): URL {
        TODO("Not yet implemented")
    }
}
