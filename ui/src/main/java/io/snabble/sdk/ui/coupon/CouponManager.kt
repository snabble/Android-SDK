package io.snabble.sdk.ui.coupon

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.coupons.Coupon

/**
 * A Coupon Manager which holds the current active Coupons as [LiveData]. There are two ways to access them:
 * - [withCurrentProject] to get the Coupons of the currently checked in project
 * - [withProject] to get the Coupons of a selected project
 */
class CouponManager private constructor(private var currentProject: Project?): LiveData<List<CouponItem>>() {
    private val observeProjectChanges = currentProject == null
    private val couponObserver = Observer<List<Coupon>> { list ->
        postValue(list
            .filter { it.type == CouponType.DIGITAL && it.image != null }
            .map { CouponItem(requireNotNull(currentProject?.id), it) }
        )
    }
    private val projectObserver = Observer<Project?> { project ->
        currentProject?.coupons?.removeObserver(couponObserver)
        currentProject = project
        currentProject?.coupons?.observeForever(couponObserver)
        updateCoupons()
    }

    init {
        if (observeProjectChanges) {
            currentProject = Snabble.checkedInProject.value
            Snabble.checkedInProject.observeForever(projectObserver)
        } else {
            currentProject?.coupons?.observeForever(couponObserver)
        }
        currentProject?.coupons?.observeForever(couponObserver)
        updateCoupons()
    }

    fun updateCoupons() = currentProject?.coupons?.update()

    companion object {
        private val currentProject by lazy { CouponManager(null) }
        /** Returns a CouponManager/LiveData with the Coupons of the currently checked in project */
        @JvmStatic
        fun withCurrentProject() = currentProject
        /** Returns a CouponManager/LiveData for the given project */
        @JvmStatic
        fun withProject(project: Project) = CouponManager(project)
        /** Check if a project has coupons setup */
        @JvmStatic
        fun hasProjectCoupons(project: Project?) = project?.urls?.get("coupons") != null
    }
}