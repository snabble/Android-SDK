package io.snabble.sdk.shopfinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.isNotNullOrBlank

/**
 * Displays the ExpandableShopList of the selected shop.
 * Hides back Button in Toolbar if only one project is available.
 * If the back Button needs to displayed extend this class and
 * setDisplayHomeAsUpEnabled to true.
 * To set a custom page title override resource string 'Snabble_Shop_Finder_title'
 * */
open class ShopListFragment : Fragment() {
    private lateinit var locationManager: CheckInLocationManager
    private lateinit var shopListRecyclerView: ExpandableShopListRecyclerView

    override fun onStart() {
        if (Snabble.projects.size == 1) {
            (context as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        locationManager = Snabble.checkInLocationManager
        return inflater.inflate(R.layout.snabble_shop_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportActionBar = (context as AppCompatActivity).supportActionBar
        val actionbarTitle = resources.getText(R.string.Snabble_Shop_Finder_title)

        if (actionbarTitle.isNotNullOrBlank()) {
            supportActionBar?.title = actionbarTitle
        }

        shopListRecyclerView = view.findViewById(R.id.recycler_view)
        shopListRecyclerView.sortByDistance(locationManager.location.value)
        shopListRecyclerView.setShopsByProjects(Snabble.projects)

        locationManager.location.observe(viewLifecycleOwner){ currentLocation ->
            shopListRecyclerView.sortByDistance(currentLocation)
        }

    }

    override fun onResume() {
        super.onResume()
        shopListRecyclerView.update()
    }

}