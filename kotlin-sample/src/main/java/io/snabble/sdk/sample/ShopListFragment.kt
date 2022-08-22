package io.snabble.sdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.snabble.sdk.Snabble
import io.snabble.sdk.location.LocationManager
import io.snabble.sdk.shopfinder.ExpandableShopListRecyclerView
import io.snabble.sdk.shopfinder.ShopfinderViewModel

class ShopListFragment : Fragment() {
    private lateinit var locationManager: LocationManager
    private lateinit var shopListRecyclerView: ExpandableShopListRecyclerView

    private val viewModel: ShopfinderViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        locationManager = LocationManager.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_shop_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isCheckedIn.observe(viewLifecycleOwner){
            //Handle stuff specific for this project
        }

        shopListRecyclerView = view.findViewById(R.id.recycler_view)

        locationManager.location.observe(viewLifecycleOwner){location ->
            shopListRecyclerView.sortByDistance(location)
        }
        shopListRecyclerView.setShopsByProjects(Snabble.projects)
    }

    override fun onResume() {
        super.onResume()
        shopListRecyclerView.update()
    }

}