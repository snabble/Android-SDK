package io.snabble.sdk.ui.coupon

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.CouponType
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import java.time.ZonedDateTime

/**
 * A Coupon Manager which holds the current active Coupons as [LiveData]. There are two ways to access them:
 * - [ofCurrentProject] to get the Coupons of the currently checked in project
 * - [ofProject] to get the Coupons of a selected project
 */
class CouponManager private constructor(private var currentProject: Project?): LiveData<List<Coupon>>() {
    private val observeProjectChanges = currentProject == null
    private val couponObserver = Observer<List<io.snabble.sdk.Coupon>> { list ->
        val now = ZonedDateTime.now()
        postValue(list.filter {
            it.type == CouponType.DIGITAL && it.image != null
        }.map { Coupon(requireNotNull(currentProject), it) }.filter {
            it.validFrom?.isBefore(now) ?: false && it.validUntil?.isAfter(now) ?: false
        })
    }
    private val projectObserver = Observer<Project?> { project ->
        project?.coupons?.removeObserver(couponObserver)
        currentProject = project
        currentProject?.coupons?.observeForever(couponObserver)
        updateCoupons()
    }

    override fun onActive() {
        if (observeProjectChanges) {
            Snabble.checkedInProject.observeForever(projectObserver)
        } else {
            currentProject?.coupons?.observeForever(couponObserver)
        }
    }

    override fun onInactive() {
        if (!hasObservers()) {
            currentProject?.coupons?.removeObserver(couponObserver)
        }
    }

    fun updateCoupons() = currentProject?.coupons?.update()

    companion object {
        private val currentProject by lazy { CouponManager(null) }
        /** Returns a CouponManager/LiveData with the Coupons of the currently checked in project */
        fun ofCurrentProject() = currentProject
        /** Returns a CouponManager/LiveData for the given project */
        fun ofProject(project: Project) = CouponManager(project)
        /** Check if a project has coupons setup */
        fun hasProjectCoupons(project: Project?) = project?.urls?.get("coupons") != null
    }
}