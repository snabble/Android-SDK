package io.snabble.sdk.shopfinder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.gms.maps.MapView
import io.snabble.sdk.ui.toolkit.R

@Keep
internal class MapPushUpBehavior @JvmOverloads constructor(context: Context? = null, attrs: AttributeSet? = null) : CoordinatorLayout.Behavior<MapView?>(context, attrs) {
    private var shopDetailsFragment: ShopDetailsFragment? = null

    fun setShopDetailsFragment(shopDetailsFragment: ShopDetailsFragment?) {
        this.shopDetailsFragment = shopDetailsFragment
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        mapView: MapView,
        dependency: View
    ): Boolean {
        return dependency.id == R.id.bottom_sheet
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        mapView: MapView,
        dependency: View
    ) {
        shopDetailsFragment?.let {
            it.setMapPadding(0, 0, 0, 0)
        }
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        mapView: MapView,
        dependency: View
    ): Boolean {
        shopDetailsFragment?.let {
            val paddingBottom = (-(dependency.y - parent.height)).toInt()
            it.setMapPadding(0, 0, 0, paddingBottom)
        }
        return true
    }
}