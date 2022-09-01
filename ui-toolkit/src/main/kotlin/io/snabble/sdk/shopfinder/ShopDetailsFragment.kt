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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
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
import io.snabble.sdk.utils.setTextOrHide
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

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
    private lateinit var mapViewPermission: View
    private lateinit var bottomSheet: View
    private lateinit var dayTable: Map<String, String>
    private lateinit var debugCheckin: TextView
    private lateinit var mapPinHome: ImageView
    private lateinit var mapPinShop: ImageView
    private lateinit var mapPinNavigate: ImageView
    private lateinit var image: ImageView
    private lateinit var address: TextView
    private lateinit var phone: TextView
    private lateinit var distance: TextView
    private lateinit var timetableTitle: View
    private lateinit var timetable: ViewGroup
    private lateinit var companyHeader: TextView
    private lateinit var companyCountry: TextView
    private lateinit var companyName: TextView
    private lateinit var companyStreet: TextView
    private lateinit var companyZip: TextView

    private var preferences: ShopfinderPreferences? = null
    private var project: Project? = null
    private var mapView: MapView? = null
    private var cachedGoogleMap: GoogleMap? = null
    private var homePinned = false
    private var storePinned = false

    private val isCheckedInToShop: Boolean
        get() = Snabble.currentCheckedInShop.value != null
            && Snabble.checkedInProject.value != null
            && Snabble.currentCheckedInShop.value?.id == shop.id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = ShopfinderPreferences.getInstance(requireContext())

        dayTable = mapOf(
            "Monday" to getString(R.string.Snabble_Shop_Details_monday),
            "Tuesday" to getString(R.string.Snabble_Shop_Details_tuesday),
            "Wednesday" to getString(R.string.Snabble_Shop_Details_wednesday),
            "Thursday" to getString(R.string.Snabble_Shop_Details_thursday),
            "Friday" to getString(R.string.Snabble_Shop_Details_friday),
            "Saturday" to getString(R.string.Snabble_Shop_Details_saturday),
            "Sunday" to getString(R.string.Snabble_Shop_Details_sunday),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val v = inflater.inflate(R.layout.snabble_shop_details_fragment, container, false)
        image = v.findViewById(R.id.image)
        address = v.findViewById(R.id.address)
        phone = v.findViewById(R.id.phone)
        distance = v.findViewById(R.id.distance)
        timetableTitle = v.findViewById(R.id.timetable_title)
        timetable = v.findViewById(R.id.timetable)
        companyHeader = v.findViewById(R.id.company_header)
        companyCountry = v.findViewById(R.id.company_country)
        companyName = v.findViewById(R.id.company_name)
        companyStreet = v.findViewById(R.id.company_street)
        companyZip = v.findViewById(R.id.company_zip)

        locationManager = LocationManager.getInstance(requireContext())
        shop = requireNotNull(arguments?.getParcelable(BUNDLE_KEY_SHOP))
        project = Snabble.projects.firstOrNull { project ->
            project.shops.any { projectShop -> projectShop.id == shop.id }
        }

        cachedGoogleMap = null
        mapViewPermission = v.findViewById(R.id.map_view_permission)
        bottomSheet = v.findViewById(R.id.bottom_sheet)

        if (preferences?.isMapsEnabled == true) {
            setupMapView(v, savedInstanceState)
        } else {
            val activateMap = v.findViewById<View>(R.id.activate_map)
            val tv = v.findViewById<TextView>(R.id.maps_notice)
            tv.text = getString(R.string.Snabble_Shop_Details_MapDisabled_title, "Google Maps")
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
            companyNotice.setTextOrHide(text)
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

        mapView?.isVisible = true
        mapControls.isVisible = true
        mapViewPermission.isVisible = false
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
        address.text = shop.street + "\n" + shop.zipCode + " " + shop.city
        address.contentDescription = getString(
            R.string.Snabble_Shop_Address_accessibility,
            shop.street,
            shop.zipCode,
            shop.city
        )
        phone.setTextOrHide(shop.phone)
        Linkify.addLinks(phone, Pattern.compile(".*"), shop.phone)
        phone.setClickDescription(R.string.Snabble_Shop_Detail_Phonenumber_accessibility)

        phone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", shop.phone, null))
            startActivity(intent)
        }

        if (Snabble.projects.size > 1) {
            project?.assets?.get("logo") { bm -> image.setImageBitmap(bm) }
        }

        if (!isCheckedInToShop) {
            locationManager.location.observe(viewLifecycleOwner) { location ->
                distance.isVisible = location != null
                location?.let { currentLocation ->
                    distance.isVisible = true
                    val distanceString = currentLocation.toLatLng()
                        .distanceTo(LatLng(shop.latitude, shop.longitude))
                        .formatDistance()
                    if (distance.text.toString() != distanceString) {
                        distance.text = distanceString
                        distance.contentDescription =
                            getString(R.string.Snabble_Shop_Distance_accessibility, distanceString)
                    }
                }
            }
        }


        if (shop.openingHours.isNullOrEmpty()) {
            timetableTitle.isVisible = false
        } else {
            timetable.removeAllViews()
            val weekDays =
                arrayListOf(
                    "Monday",
                    "Tuesday",
                    "Wednesday",
                    "Thursday",
                    "Friday",
                    "Saturday",
                    "Sunday"
                )
            shop.openingHours.sortBy { it.opens }
            shop.openingHours.sortBy { weekDays.indexOf(it.dayOfWeek) }
            shop.openingHours?.forEach { spec ->
                val day = TextView(context)
                if (weekDays.contains(spec.dayOfWeek)) {
                    day.text = "${dayTable[spec.dayOfWeek]}: "
                    weekDays.remove(spec.dayOfWeek)
                } else {
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
                    locationManager.startTrackingLocation()
                } else {
                    checkInManager.shop = shop
                    Snabble.checkedInProject.setValue(project)
                    checkInManager.stopUpdating()
                    locationManager.stopTrackingLocation()
                }
                updateDebugCheckinText()
                updateShopDetails(view)
            }
        } else {
            debugCheckin.isVisible = false
        }

        val startScanner = view.findViewById<Button>(R.id.start_scanner)
        val buttonTitle = resources.getText(R.string.Snabble_Scanner_start)

        if (isCheckedInToShop) {
            startScanner.setTextOrHide(buttonTitle)
            distance.isVisible = false
        } else {
            startScanner.isVisible = false
            distance.isVisible = true
        }

        startScanner.setOneShotClickListener {
            SnabbleUiToolkit.executeAction(
                requireContext(),
                SnabbleUiToolkit.Event.DETAILS_SHOP_BUTTON_ACTION
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
                companyHeader.isVisible = false
                companyZip.isVisible = false
            }
        } else {
            companyHeader.isVisible = false
        }
    }

    private val localTimeParser = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

    private val formatter: SimpleDateFormat by lazy {
        val pattern = if (DateFormat.is24HourFormat(requireContext())) "HH:mm" else "hh:mm a"
        SimpleDateFormat(pattern, Locale.getDefault())
    }

    private fun String.toLocalTime(): String {
        val hourSecondsCount = 5
        return try {
            val date = localTimeParser.parse(this) ?: return take(hourSecondsCount)
            formatter.format(date)
        } catch (e: ParseException) {
            take(hourSecondsCount)
        }
    }

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
        val actionbarTitle = resources.getText(R.string.Snabble_Shop_Detail_title)

        if (actionbarTitle == "Details") {
            supportActionBar?.title = shop.name
        } else {
            supportActionBar?.title = actionbarTitle
        }
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
