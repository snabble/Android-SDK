package io.snabble.sdk.shopfinder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.snabble.sdk.Snabble
import io.snabble.sdk.location.LocationManager
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.isNotNullOrBlank

open class ShopListFragment : Fragment() {
    private lateinit var locationManager: LocationManager
    private lateinit var shopListRecyclerView: ExpandableShopListRecyclerView

    companion object {
        val shopsOnly = Snabble.projects.size == 1
    }

    //Todo: fix delay?
    override fun onStart() {
        if (shopsOnly) {
            (context as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        locationManager = LocationManager.getInstance(requireContext())
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
        locationManager.location.observe(viewLifecycleOwner) { location ->
            shopListRecyclerView.sortByDistance(location)
        }
        shopListRecyclerView.setShopsByProjects(Snabble.projects)
    }

    override fun onResume() {
        super.onResume()
        shopListRecyclerView.update()
        shopListRecyclerView.sortByDistance(locationManager.getLastLocation())
    }

}