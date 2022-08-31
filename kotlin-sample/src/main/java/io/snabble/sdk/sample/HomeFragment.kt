package io.snabble.sdk.sample

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        root.findViewById<Button>(R.id.show_payment_credentials).setOnClickListener {
            findNavController().navigate(R.id.navigation_payment_credentials)
        }

        // Start dialog for location permission
        root.findViewById<Button>(R.id.request_location_permission).setOnClickListener {
            (requireActivity() as MainActivity).locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return root
    }
}