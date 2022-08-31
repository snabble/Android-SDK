package io.snabble.sdk.shopfinder


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.snabble.accessibility.isTalkBackActive
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.location.LocationManager
import io.snabble.sdk.location.distanceTo
import io.snabble.sdk.location.formatDistance
import io.snabble.sdk.location.toLatLng
import io.snabble.sdk.shopfinder.utils.ISO3Utils.getDisplayNameByIso3Code
import io.snabble.sdk.shopfinder.utils.OneShotClickListener
import io.snabble.sdk.shopfinder.utils.ShopfinderPreferences
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.behavior
import io.snabble.sdk.ui.utils.dpInPx
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.utils.isNotNullOrBlank
import io.snabble.sdk.utils.setTextOrHide
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

// TODO: Implement ShopDetails from Teo e.g. door opening and alwyas open text

/**
 * Displays the details of the selected shop.
 *
 * To set a custom page title override resource string 'Snabble_Shop_Details_title'.
 * By default the title matches the current shop title.
 * Keep in mind that if this string is set it is used for every details shop page.
 *
 * To set up a the details button with a resource string via 'Snabble_Shop_Details_button'.
 * The button fires a SnabbleUiToolkit 'SHOW_DETAILS_BUTTON_ACTION' event. Simply set up
 * setUiAction for this event to declare the action for the button click.
 * */
open class ShopDetailsFragment : Fragment() {
    private lateinit var locationManager: LocationManager
    private lateinit var shop: Shop
    private var preferences: ShopfinderPreferences? = null
    private var project: Project? = null
    private var mapView: MapView? = null
    private var cachedGoogleMap: GoogleMap? = null
    private lateinit var mapViewPermission: View
    private lateinit var bottomSheet: View
    private lateinit var dayTable: Map<String, String>
    private lateinit var debugCheckin: TextView
    private lateinit var mapPinHome: ImageView
    private lateinit var mapPinShop: ImageView
    private lateinit var mapPinNavigate: ImageView
    private var homePinned = false
    private var storePinned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = LocationManager.getInstance(requireContext())
        locationManager.startTrackingLocation()

        preferences = ShopfinderPreferences.getInstance(requireContext())

        //Todo: insert Strings
        dayTable = mapOf(
            "Monday" to "Monday",
            "Tuesday" to "Tuesday",
            "Wednesday" to "Wednesday",
            "Thursday" to "Thursday",
            "Friday" to "Friday",
            "Saturday" to "Saturday",
            "Sunday" to "Sunday",
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.snabble_shop_details_fragment, container, false)

        shop = requireNotNull(arguments?.getParcelable(BUNDLE_KEY_SHOP))
        project =
            Snabble.projects.firstOrNull() { project -> project.shops.any { projectShop -> projectShop.id == shop.id } }

        cachedGoogleMap = null
        mapViewPermission = v.findViewById(R.id.map_view_permission)
        bottomSheet = v.findViewById(R.id.bottom_sheet)
        /**Shared pref Value the same? */
        if (preferences?.isMapsEnabled == true) {
            setupMapView(v, savedInstanceState)
        } else {
            val activateMap = v.findViewById<View>(R.id.activate_map)
            val tv = v.findViewById<TextView>(R.id.maps_notice)
            tv.text = getString(R.string.ShopDetails_ForbiddenMaps_Title, "Google Maps")
            activateMap.setOnClickListener {
                if (preferences?.hasGoogleMapsKey == true) {
                    preferences?.isMapsEnabled = true
                    setupMapView(v, savedInstanceState)
                    mapView?.onStart()
                    val lp = mapViewPermission.layoutParams as MarginLayoutParams
                    setMapPadding(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
                } else {
                    Toast.makeText(context, "No Google Maps API key found!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            mapViewPermission.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapViewPermission.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        applyBottomSheetPeekHeight(v)
                    }
                })
        }
        mapPinHome = v.findViewById(R.id.map_pin_home)
        mapPinHome.setOnClickListener(object : OneShotClickListener() {
            override fun click() {
                if (!homePinned) {
                    zoomToHome()
                }
            }
        })
        mapPinShop = v.findViewById(R.id.map_pin_store)
        mapPinShop.setOnClickListener(object : OneShotClickListener() {
            override fun click() {
                if (!storePinned) {
                    zoomToShop(shop, true)
                }
            }
        })

        mapPinNavigate = v.findViewById(R.id.map_directions)
        mapPinNavigate.setOneShotClickListener {
            val uri = Uri.parse("google.navigation:q=${shop.latitude},${shop.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (isIntentAvailable(requireContext(), intent)) {
                startActivity(intent)
            }
            SnabbleUiToolkit.executeAction(
                requireContext().applicationContext,
                SnabbleUiToolkit.Event.START_NAVIGATION
            )
        }
        val companyNotice = v.findViewById<TextView>(R.id.company_notice)

        project?.let { project ->
            val text = project.getText("companyNotice")
            if (text != null) {
                companyNotice.visibility = View.VISIBLE
                companyNotice.text = text
            } else {
                companyNotice.visibility = View.GONE
            }
        }

        applyBottomSheetPeekHeight(v)
        updateShopDetails(v)
        return v
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun setupMapView(v: View, savedInstanceState: Bundle?) {
        mapView = v.findViewById(R.id.map_view)
        val mapControls = v.findViewById<View>(R.id.map_controls)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(BUNDLE_KEY_MAPVIEW)
        }
        mapView?.visibility = View.VISIBLE
        mapControls.visibility = View.VISIBLE
        mapViewPermission.visibility = View.GONE
        mapView?.onCreate(mapViewBundle)
        mapView?.viewTreeObserver?.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    applyBottomSheetPeekHeight(v)
                }
            })
        accessGoogleMap { googleMap ->
            project?.let { project ->
                for (shop in project.shops) {
                    project.assets.get("icon") { bitmap: Bitmap? ->
                        if (bitmap != null) {
                            val bd = BitmapDescriptorFactory.fromBitmap(bitmap)
                            val latLng = LatLng(shop.latitude, shop.longitude)
                            googleMap.addMarker(
                                MarkerOptions()
                                    .icon(bd)
                                    .anchor(0.5f, 0.5f)
                                    .position(latLng)
                            )
                        }
                    }
                }
            }
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            googleMap.uiSettings.isMapToolbarEnabled = false
            if (preferences?.isInDarkMode == true) {
                googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(),
                        R.raw.google_maps_dark_style
                    )
                )
            }
            if (LocationManager.getInstance(requireContext()).isLocationAvailable()) {
                googleMap.isMyLocationEnabled = true
            }
            googleMap.setOnCameraMoveStartedListener { reason: Int ->
                if (reason != OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
                    homePinned = false
                    mapPinHome.setImageResource(R.drawable.snabble_map_user_postition)
                    storePinned = false
                    mapPinShop.setImageResource(R.drawable.snabble_map_store)
                }
            }
            zoomToShop(shop, false)
        }
        val pushUpBehavior = mapView?.behavior as? MapPushUpBehavior
        pushUpBehavior?.setShopDetailsFragment(this)
    }

    //Todo refactor
    private fun applyBottomSheetPeekHeight(v: View) {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        var desiredPeekHeight = 200.dpInPx
        var view = v.findViewById<View>(R.id.start_scanner)
        if (!view.isVisible) {
            view = v.findViewById(R.id.phone)
            if (!view.isVisible) {
                view = v.findViewById(R.id.timetable_title)
                if (!view.isVisible) {
                    view = v.findViewById(R.id.company_header)
                }
            }
            desiredPeekHeight = view.bottom - 12.dpInPx
        }

        if (requireContext().isTalkBackActive) {
            bottomSheetBehavior.halfExpandedRatio = 0.5f
            bottomSheetBehavior.isFitToContents = false
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        } else {
            bottomSheetBehavior.peekHeight = desiredPeekHeight
        }
    }

    @SuppressLint("SetTextI18n", "MissingPermission")
    fun updateShopDetails(view: View) {
        val image = view.findViewById<ImageView>(R.id.image)
        val address = view.findViewById<TextView>(R.id.address)
        val phone = view.findViewById<TextView>(R.id.phone)
        val distance = view.findViewById<TextView>(R.id.distance)
        val timetableTitle = view.findViewById<View>(R.id.timetable_title)
        val timetable = view.findViewById<ViewGroup>(R.id.timetable)
        val companyHeader = view.findViewById<TextView>(R.id.company_header)
        val companyCountry = view.findViewById<TextView>(R.id.company_country)
        val companyName = view.findViewById<TextView>(R.id.company_name)
        val companyStreet = view.findViewById<TextView>(R.id.company_street)
        val companyZip = view.findViewById<TextView>(R.id.company_zip)
        address.text = shop.street + "\n" + shop.zipCode + " " + shop.city
        address.contentDescription = getString(
            R.string.ShopDetails_Accessibility_address,
            shop.street,
            shop.zipCode,
            shop.city
        )
        phone.text = getString(R.string.ShopDetails_phone, shop.phone)
        Linkify.addLinks(phone, Pattern.compile(".*"), shop.phone)
        phone.setClickDescription(R.string.ShopDetails_Accessibility_startCall)
        phone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", shop.phone, null))
            startActivity(intent)
        }

        if (Snabble.projects.size > 1) {
            project?.assets?.get("logo") { bm -> image.setImageBitmap(bm) }
        }

        locationManager.location.observe(viewLifecycleOwner) { location ->
            distance.isVisible = location != null
            location?.let { currentLocation ->
                distance.visibility = View.VISIBLE
                val distanceString = currentLocation.toLatLng()
                    .distanceTo(LatLng(shop.latitude, shop.longitude))
                    .formatDistance()
                if (distance.text.toString() != distanceString) {
                    distance.text = distanceString
                    distance.contentDescription =
                        getString(R.string.ShopDetails_Accessibility_distance, distanceString)
                }
            }
        }

        val parser = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.GERMANY)
        val target = (if (DateFormat.is24HourFormat(requireContext()))
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        else DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())).toFormat()
        fun String.toLocalTime(): String = target.format(parser.parse(this))

        if (shop.openingHours.isNullOrEmpty()) {
            timetableTitle.visibility = View.GONE
        } else {
            timetable.removeAllViews()
            val weekDays = arrayListOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
            shop.openingHours.sortBy { weekDays.indexOf(it.dayOfWeek)}

            shop.openingHours?.forEach { spec ->
                val day = TextView(context)
                if (weekDays.contains(spec.dayOfWeek)) {
                    day.text = "${dayTable[spec.dayOfWeek]}: "
                    weekDays.remove(spec.dayOfWeek)
                }else{
                    day.text = ""
                }
                timetable.addView(
                    day,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val textView = TextView(context)
                textView.text = spec.opens.toLocalTime() + " \u2013 " + spec.closes.toLocalTime()
                timetable.addView(
                    textView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        debugCheckin = view.findViewById(R.id.debug_checkin)
        val debug = preferences?.isDebuggingAvailable == true
        val checkinAllowed =
            preferences?.projectCode != null && project?.id == Snabble.projects[0].id
        val checkInManager = Snabble.checkInManager
        if (checkinAllowed || debug) {
            debugCheckin.isVisible = true
            updateDebugCheckinText()
            debugCheckin.setOneShotClickListener {
                if (isCheckedInToShop) {
                    Snabble.checkedInProject.setValue(null)
                    checkInManager.shop = null
                    checkInManager.startUpdating()
                } else {
                    checkInManager.shop = shop
                    Snabble.checkedInProject.setValue(project)
                    checkInManager.stopUpdating()
                    Toast.makeText(context, "Checkin: ${shop.name}", Toast.LENGTH_LONG).show()
                }
                updateDebugCheckinText()
                updateShopDetails(view)
            }
        } else {
            debugCheckin.visibility = View.GONE
        }

        val youAreHere = view.findViewById<View>(R.id.you_are_here)
        youAreHere.isVisible = isCheckedInToShop

        val startScanner = view.findViewById<Button>(R.id.start_scanner)
        val buttonTitle = resources.getText(R.string.Snabble_Shop_Details_button)

        if (isCheckedInToShop) {
            startScanner.setTextOrHide(buttonTitle)
        } else {
            startScanner.isVisible = false
        }

        startScanner.setOneShotClickListener {
            SnabbleUiToolkit.executeAction(
                requireContext(),
                SnabbleUiToolkit.Event.SHOW_DETAILS_BUTTON_ACTION
            )
        }

        if (Snabble.projects.size > 1) {
            val company = project?.company
            companyCountry.setTextOrHide(project?.company?.country?.let(::getDisplayNameByIso3Code))
            companyName.setTextOrHide(project?.company?.name)
            companyStreet.setTextOrHide(project?.company?.street)
            if (company != null) {
                companyZip.text = company.zip + " " + company.city
            } else {
                companyHeader.visibility = View.GONE
                companyZip.visibility = View.GONE
            }
        } else {
            companyHeader.visibility = View.GONE
        }
    }

    private val isCheckedInToShop: Boolean
        get() =
            Snabble.checkInManager.shop != null && Snabble.checkInManager.project != null && Snabble.checkInManager.shop?.id == shop.id

    @SuppressLint("SetTextI18n")
    private fun updateDebugCheckinText() {
        debugCheckin.text = if (isCheckedInToShop) {
            "Checkout"
        } else {
            "Checkin"
        }
    }

    private fun accessGoogleMap(run: (googleMap: GoogleMap) -> Unit) {
        val cachedMap = cachedGoogleMap
        if (cachedMap == null) {
            mapView?.getMapAsync { map ->
                cachedGoogleMap = map
                run(map)
            }
        } else {
            run(cachedMap)
        }
    }

    fun setMapPadding(left: Int, top: Int, right: Int, bottom: Int) {
        accessGoogleMap { googleMap ->
            val cameraPosition = googleMap.cameraPosition
            googleMap.setPadding(left, top, right, bottom)
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun zoomToLocation(latLng: LatLng, animate: Boolean) {
        accessGoogleMap { googleMap ->
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                    .target(latLng)
                    .zoom(17f)
                    .bearing(0f)
                    .tilt(0f)
                    .build()
            )
            if (animate) {
                googleMap.animateCamera(cameraUpdate)
            } else {
                googleMap.moveCamera(cameraUpdate)
            }
        }
    }

    fun zoomToHome() {
        if (storePinned) {
            storePinned = false
            mapPinShop.setImageResource(R.drawable.snabble_map_store)
        }
        mapPinHome.setImageResource(R.drawable.snabble_map_user_position_active)
        homePinned = true
        zoomToLocation(locationManager.location.value!!.toLatLng(), true)
    }

    private fun zoomToShop(shop: Shop, animate: Boolean) {
        if (homePinned) {
            homePinned = false
            mapPinHome.setImageResource(R.drawable.snabble_map_user_postition)
        }
        mapPinShop.setImageResource(R.drawable.snabble_map_store_active)
        storePinned = true
        zoomToLocation(LatLng(shop.latitude, shop.longitude), animate)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()

        val supportActionBar = (context as AppCompatActivity).supportActionBar
        val actionbarTitle = resources.getText(R.string.Snabble_Shop_Details_title)

        if (actionbarTitle.isNotNullOrBlank()) {
            supportActionBar?.title = actionbarTitle
        } else {
            supportActionBar?.title = shop.name
        }

//        shopManager?.addOnCheckInStateChangedListener(this)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
//        shopManager?.removeOnCheckInStateChangedListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mapView != null) {
            var mapViewBundle = outState.getBundle(BUNDLE_KEY_MAPVIEW)
            if (mapViewBundle == null) {
                mapViewBundle = Bundle()
                outState.putBundle(BUNDLE_KEY_MAPVIEW, mapViewBundle)
            }
            mapView?.onSaveInstanceState(mapViewBundle)
        }
    }

    companion object {
        const val BUNDLE_KEY_SHOP = "shop"
        private const val BUNDLE_KEY_MAPVIEW = "mapView"
        const val KEY_MAPS_ENABLED = "mapsEnabled"

        @SuppressLint("QueryPermissionsNeeded")
        fun isIntentAvailable(context: Context, intent: Intent): Boolean {
            val packageManager = context.packageManager
            val list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list.isNotEmpty()
        }
    }
}