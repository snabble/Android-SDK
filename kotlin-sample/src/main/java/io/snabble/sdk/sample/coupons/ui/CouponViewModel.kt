package io.snabble.sdk.sample.coupons.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.ui.coupon.CouponItem
import io.snabble.sdk.ui.coupon.CouponManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CouponViewModel : ViewModel() {

    private val couponManager by lazy { CouponManager.withCurrentProject() }

    private val _coupons = MutableStateFlow<List<CouponItem>>(emptyList())
    val coupons: StateFlow<List<CouponItem>> = _coupons.asStateFlow()

    private val _event = MutableStateFlow<CouponEvent?>(null)
    val event: StateFlow<CouponEvent?> = _event.asStateFlow()

    init {
        viewModelScope.launch {
            couponManager.asFlow().collectLatest {
                _coupons.tryEmit(it)
            }
        }
    }

    fun onEvent(event: CouponEvent) {
        _event.update { event }
    }

    fun eventHandled() {
        _event.update { null }
    }

    fun updateCoupons() = couponManager.updateCoupons()
}

sealed interface CouponEvent
data class ShowCoupon(val couponItem: CouponItem) : CouponEvent
