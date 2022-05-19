package io.snabble.sdk.ui.coupon

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.CouponType
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import java.time.ZonedDateTime

object CouponManager {
    val coupons = MutableLiveData<List<Coupon>?>()
    private var currentProject: Project? = null
    private val couponObserver = Observer<List<io.snabble.sdk.Coupon>> { list ->
        val now = ZonedDateTime.now()
        coupons.postValue(list.filter {
            it.type == CouponType.DIGITAL && it.image != null
        }.map { Coupon(requireNotNull(currentProject), it) }.filter {
            it.validFrom?.isBefore(now) ?: false && it.validUntil?.isAfter(now) ?: false
        })
    }

    init {
        Snabble.checkedInProject.observeForever { project ->
            project?.coupons?.removeObserver(couponObserver)
            currentProject = project
            currentProject?.coupons?.observeForever(couponObserver)
            updateCoupons()
        }
    }

    fun updateCoupons() = currentProject?.coupons?.update()
}