package io.snabble.sdk.screens.shopfinder

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
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
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.screens.shopfinder.utils.ISO3Utils.getDisplayNameByIso3Code
import io.snabble.sdk.screens.shopfinder.utils.ShopFinderPreferences
import io.snabble.sdk.screens.shopfinder.utils.distanceTo
import io.snabble.sdk.screens.shopfinder.utils.formatDistance
import io.snabble.sdk.screens.shopfinder.utils.toLatLng
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.behavior
import io.snabble.sdk.ui.utils.dpInPx
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.utils.PhoneNumberUtils
import io.snabble.sdk.utils.isNotNullOrBlank
import io.snabble.sdk.utils.setTextOrHide
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
 */
open class ShopDetailsFragment : Fragment() {

    private val dayTable: Map<String, String>
    private val sortedWeek: List<String>
    private lateinit var locationManager: CheckInLocationManager
    private lateinit var shop: Shop
    private lateinit var mapViewPermission: View
    private lateinit var bottomSheet: View
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
    private lateinit var companyNotice: TextView
    private lateinit var companyHeader: TextView
    private lateinit var companyCountry: TextView
    private lateinit var companyName: TextView
    private lateinit var companyStreet: TextView
    private lateinit var companyZip: TextView

    private var preferences: ShopFinderPreferences? = null
    private var project: Project? = null
    private var mapView: MapView? = null
    private var cachedGoogleMap: GoogleMap? = null
    private var homePinned = false
    private var storePinned = false

    private val isMultiProject: Boolean
        get() = Snabble.projects.size > 1

    private val isCheckedInToShop: Boolean
        get() = Snabble.currentCheckedInShop.value != null
                && Snabble.checkedInProject.value != null
                && Snabble.currentCheckedInShop.value?.id == shop.id

    init {
        val usWeekdays = DateFormatSymbols.getInstance(Locale.US).weekdays.drop(1)
        val localWeekdays = DateFormatSymbols.getInstance().weekdays.drop(1)

        dayTable = usWeekdays.mapIndexed { index, englishWeekday ->
            englishWeekday to localWeekdays[index]
        }.toMap()

        sortedWeek = usWeekdays
        val firstDayOfWeek = GregorianCalendar.getInstance().firstDayOfWeek
        Collections.rotate(sortedWeek, -(firstDayOfWeek - 1))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.snabble_shop_details_fragment, container, false).apply {
        val view = this

        image = findViewById(R.id.image)
        address = findViewById(R.id.address)
        phone = findViewById(R.id.phone)
        distance = findViewById(R.id.distance)
        timetableTitle = findViewById(R.id.timetable_title)
        timetable = findViewById(R.id.timetable)
        companyHeader = findViewById(R.id.company_header)
        companyCountry = findViewById(R.id.company_country)
        companyName = findViewById(R.id.company_name)
        companyStreet = findViewById(R.id.company_street)
        companyZip = findViewById(R.id.company_zip)

        preferences = ShopFinderPreferences.getInstance(requireContext())
        locationManager = Snabble.checkInLocationManager
        shop = getShop()
        project = Snabble.projects.firstOrNull { project ->
            project.shops.any { projectShop -> projectShop.id == shop.id }
        }

        cachedGoogleMap = null
        mapViewPermission = findViewById(R.id.map_view_permission)
        bottomSheet = findViewById(R.id.bottom_sheet)

        setUpMapViewPermissionLayout(savedInstanceState, view)

        mapPinHome = findViewById(R.id.map_pin_home)
        mapPinHome.setOneShotClickListener {
            if (!homePinned) {
                zoomToHome()
            }
        }

        mapPinShop = findViewById(R.id.map_pin_store)
        mapPinShop.setOneShotClickListener {
            if (!storePinned) {
                zoomToShop(shop, true)
            }
        }

        mapPinNavigate = findViewById(R.id.map_directions)
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

        companyNotice = findViewById(R.id.company_notice)
        project?.let { project ->
            val text = project.getText("companyNotice")
            companyNotice.setTextOrHide(text)
        }

        applyBottomSheetPeekHeight(view)
        updateShopDetails(view)

    }

    private fun getShop(): Shop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arguments?.getParcelable(BUNDLE_KEY_SHOP, Shop::class.java)
    } else {
        @Suppress("DEPRECATION")
        arguments?.getParcelable(BUNDLE_KEY_SHOP)
    }
        ?: throw IllegalArgumentException("Missing necessary bundle args")

    // Returns a textview message to activate google maps if it is not enabled otherwise it sets up the mapview.
    // The activation is only possible if the project contains a google maps api key
    private fun setUpMapViewPermissionLayout(savedInstanceState: Bundle?, view: View) {
        if (preferences?.isMapsEnabled == true && preferences?.hasGoogleMapsKey == true) {
            setupMapView(view, savedInstanceState)
        } else {
            val activateMapButton = view.findViewById<View>(R.id.activate_map)
            val activateMessage = view.findViewById<TextView>(R.id.maps_notice)
            activateMessage.text = getString(R.string.Snabble_Shop_Details_MapDisabled_title, "Google Maps")
            activateMapButton.setOnClickListener {
                if (preferences?.hasGoogleMapsKey == true) {
                    preferences?.isMapsEnabled = true
                    setupMapView(view, savedInstanceState)
                    mapView?.onStart()
                    val lp = mapViewPermission.layoutParams as MarginLayoutParams
                    setMapPadding(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
                } else {
                    Toast.makeText(context, "No Google Maps API key found!", Toast.LENGTH_SHORT).show()
                }
            }
            mapViewPermission.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mapViewPermission.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        applyBottomSheetPeekHeight(view)
                    }
                })
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupMapView(v: View, savedInstanceState: Bundle?) {
        mapViewPermission.isVisible = false

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(BUNDLE_KEY_MAPVIEW)
        }

        val mapControls = v.findViewById<View>(R.id.map_controls)
        mapControls.isVisible = true

        mapView = v.findViewById(R.id.map_view)
        mapView?.isVisible = true
        mapView?.onCreate(mapViewBundle)
        mapView?.viewTreeObserver?.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    applyBottomSheetPeekHeight(v)
                }
            })

        val pushUpBehavior = mapView?.behavior as? MapPushUpBehavior
        pushUpBehavior?.setShopDetailsFragment(this)

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

            if (locationManager.isLocationAvailable()) {
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
    }

    //Todo refactor
    private fun applyBottomSheetPeekHeight(v: View) {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        var desiredPeekHeight = 200.dpInPx

        val detailsButton = v.findViewById<View>(R.id.start_scanner)
        if (detailsButton.isVisible) {
            desiredPeekHeight = detailsButton.bottom + 12.dpInPx
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

        if (shop.phone.isNotNullOrBlank()) {
            phone.text = PhoneNumberUtils.convertPhoneNumberToUrlSpan(shop.phone)
            phone.movementMethod = LinkMovementMethod.getInstance()
            phone.setClickDescription(R.string.Snabble_Shop_Detail_Phonenumber_accessibility)
        } else {
            phone.isVisible = false
        }

        if (isMultiProject) {
            project?.assets?.get("logo") { bm -> image.setImageBitmap(bm) }
        }

        if (!isCheckedInToShop) {
            if (locationManager.location.value == null) {
                distance.isVisible = false
            }
            locationManager.location.observe(viewLifecycleOwner) { location ->
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

        setUpTimeTable()
        setUpDebugCheckIn(view)

        val startScanner = view.findViewById<Button>(R.id.start_scanner)
        val startScannerTitle = resources.getText(R.string.Snabble_Scanner_start)

        if (isCheckedInToShop) {
            startScanner.setTextOrHide(startScannerTitle)
            distance.isVisible = false
        } else {
            startScanner.isVisible = false
        }

        startScanner.setOneShotClickListener {
            SnabbleUiToolkit.executeAction(
                requireContext(),
                SnabbleUiToolkit.Event.DETAILS_SHOP_BUTTON_ACTION
            )
        }

        if (isMultiProject) {
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

    @SuppressLint("MissingPermission")
    private fun setUpDebugCheckIn(view: View) {
        debugCheckin = view.findViewById(R.id.debug_checkin)
        val debug = preferences?.isDebuggingAvailable == true
        val checkinAllowed =
            preferences?.projectCode != null && project?.id == Snabble.projects[0].id
        val checkInManager = Snabble.checkInManager

        if (checkinAllowed || debug) {
            debugCheckin.isVisible = true
            updateDebugCheckInText()
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
                updateDebugCheckInText()
                updateShopDetails(view)
            }
        } else {
            debugCheckin.isVisible = false
        }
    }

    private fun updateDebugCheckInText() {
        debugCheckin.text = if (isCheckedInToShop) {
            "Checkout"
        } else {
            "Checkin"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUpTimeTable() {
        if (shop.openingHours.isNullOrEmpty()) {
            timetableTitle.isVisible = false
        } else {
            timetable.removeAllViews()
            val weekDays = sortedWeek.toMutableList()
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
        locationManager.location.observe(viewLifecycleOwner) { currentLocation ->
            currentLocation?.let {
                if (storePinned) {
                    storePinned = false
                    mapPinShop.setImageResource(R.drawable.snabble_map_store)
                }
                mapPinHome.setImageResource(R.drawable.snabble_map_user_position_active)
                homePinned = true
                zoomToLocation(it.toLatLng(), true)
            }
        }
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

        setupActionBar()
    }

    private fun setupActionBar() {
        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar ?: return

        val toolbarTitle = resources.getText(R.string.Snabble_Shop_Detail_title)
            .let { it.ifBlank { shop.name } }

        supportActionBar.title = toolbarTitle
        supportActionBar.setDisplayHomeAsUpEnabled(true)
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
        mapView?.onStop()

        super.onStop()
    }

    override fun onDestroyView() {
        mapView?.onDestroy()

        super.onDestroyView()
    }

    override fun onLowMemory() {
        mapView?.onLowMemory()

        super.onLowMemory()
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

        @SuppressLint("QueryPermissionsNeeded")
        fun isIntentAvailable(context: Context, intent: Intent): Boolean {
            val packageManager = context.packageManager
            val list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list.isNotEmpty()
        }
    }
}
