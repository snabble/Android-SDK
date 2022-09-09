package io.snabble.sdk.shopfinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.shopfinder.shoplist.ExpandableShopListRecyclerView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupActionBar()
        locationManager = Snabble.checkInLocationManager
        return inflater.inflate(R.layout.snabble_shop_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var lastLocation = locationManager.location.value
        shopListRecyclerView = view.findViewById(R.id.recycler_view)
        shopListRecyclerView.sortByDistance(lastLocation)
        shopListRecyclerView.setShopsByProjects(Snabble.projects)

        locationManager.location.observe(viewLifecycleOwner) { currentLocation ->
            currentLocation ?: return@observe

            if (currentLocation != lastLocation) lastLocation = currentLocation
            shopListRecyclerView.sortByDistance(currentLocation)
            shopListRecyclerView.setShopsByProjects(Snabble.projects)
        }
    }

    override fun onResume() {
        super.onResume()
        shopListRecyclerView.update()
    }

    private fun setupActionBar() {
        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar ?: return

        if (Snabble.projects.size == 1) {
            supportActionBar.setDisplayHomeAsUpEnabled(false)
        }

        val actionbarTitle = resources.getText(R.string.Snabble_Shop_Finder_title)
        if (actionbarTitle.isNotNullOrBlank()) {
            supportActionBar.title = actionbarTitle
        }
    }
}
