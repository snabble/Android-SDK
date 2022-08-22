package io.snabble.sdk.shopfinder


import io.snabble.sdk.Shop
import android.widget.TextView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.content.Intent
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.Snabble
import io.snabble.sdk.location.LocationManager
import io.snabble.sdk.location.distanceTo
import io.snabble.sdk.location.formatDistance
import io.snabble.sdk.location.toLatLng
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.*
import io.snabble.sdk.shopfinder.utils.ISO3Utils.getDisplayNameByIso3Code
import io.snabble.sdk.shopfinder.utils.OneShotClickListener

class ShopDetailsFragment : Fragment(){
    private lateinit var locationManager: LocationManager
    private lateinit var shop: Shop
    private var mapView: MapView? = null
    private var cachedGoogleMap: GoogleMap? = null
    private lateinit var mapViewPermission: View
    private lateinit var bottomSheet: View
    private lateinit var dayTable: Map<String, String>
    private lateinit var debugCheckin: TextView
    private lateinit var mapPinHome: ImageView
    private lateinit var mapPinNavigate: ImageView
    private var homePinned = false

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[ShopfinderViewModel::class.java]
    }

    val isInDarkMode: Boolean
        get() {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }
    val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext)

    val debugSharedPreferences : SharedPreferences
    get() = requireContext().getSharedPreferences("DebugManager", Context.MODE_PRIVATE)

    var isHiddenMenuAvailable: Boolean
        get() = debugSharedPreferences.getBoolean(KEY_HIDDEN_MENU_AVAILABLE, BuildConfig.DEBUG || projectCode != null)
        set(available) = debugSharedPreferences.edit().putBoolean(KEY_HIDDEN_MENU_AVAILABLE, available).apply()

    var isDebuggingAvailable: Boolean
        get() = debugSharedPreferences.getBoolean(KEY_DEBUGGING_AVAILABLE, BuildConfig.DEBUG)
        set(available) {
            isHiddenMenuAvailable = available
            debugSharedPreferences.edit().putBoolean(KEY_DEBUGGING_AVAILABLE, available).apply()
        }
    var projectCode: String?
        get() = sharedPreferences.getString(KEY_PROJECT_CODE, null)
        set(projectCode) {
            sharedPreferences
                .edit()
                .putString(KEY_PROJECT_CODE, projectCode)
                .apply()
        }

    private var isMapsEnabled: Boolean
        get() = sharedPreferences
            .getBoolean(KEY_MAPS_ENABLED, true)
        set(enabled) {
            sharedPreferences
                .edit()
                .putBoolean(KEY_MAPS_ENABLED, enabled)
                .apply()
        }


    //onCreateScreen analyticsname is missing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = LocationManager.getInstance(requireContext())
        locationManager.startTrackingLocation()

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
        shop = viewModel.currentShop.value!!
        cachedGoogleMap = null
        mapViewPermission = v.findViewById(R.id.map_view_permission)
        bottomSheet = v.findViewById(R.id.bottom_sheet)
        /**Shared pref Value the same? */
        if (isMapsEnabled) {
            setupMapView(v, savedInstanceState)
        } else {
            val activateMap = v.findViewById<View>(R.id.activate_map)
            val tv = v.findViewById<TextView>(R.id.maps_notice)
            tv.text = getString(R.string.ShopDetails_ForbiddenMaps_Title, "Google Maps")
            activateMap.setOnClickListener {
                requireContext().packageManager.getApplicationInfo(requireContext().packageName, PackageManager.GET_META_DATA)
                    .metaData.get("com.google.android.geo.API_KEY")?.let {
                isMapsEnabled = true
                setupMapView(v, savedInstanceState)
                mapView?.onStart()
                val lp = mapViewPermission.layoutParams as MarginLayoutParams
                setMapPadding(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)}
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
                    zoomToShop(shop, true)
                }
            }
        })
        mapPinNavigate = v.findViewById(R.id.map_pin_navigate)
        mapPinNavigate.setOneShotClickListener {
            val uri = Uri.parse("google.navigation:q=${shop.latitude},${shop.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (isIntentAvailable(requireContext(), intent)) {
                startActivity(intent)
            }
//            TODO: implement later
//            requireContext().analytics.event(Analytics.Event.NavigationStarted)
        }
        val companyNotice = v.findViewById<TextView>(R.id.company_notice)
        val project = viewModel.currentProject.value

//      Second way to skip Viemodel
//      val project2 = Snabble.projects.firstOrNull() { project -> project.shops.any { projectShop -> projectShop.id == shop.id } }

        if (project != null) {
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
                val project =
                    Snabble.projects.firstOrNull() { project -> project.shops.any { projectShop -> projectShop.id == shop.id } }
                project?.let {
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
                if (isInDarkMode) {
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
                        mapPinHome.setImageResource(R.drawable.ic_map_home)
                    }
                }
                zoomToShop(shop, false)
            }
            val pushUpBehavior = mapView?.behavior as? MapPushUpBehavior
            pushUpBehavior?.setShopDetailsFragment(this)
    }

    private fun applyBottomSheetPeekHeight(v: View) {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        var desiredPeekHeight: Int
        var view = v.findViewById<View>(R.id.start_scanner)
        if (view.visibility == View.GONE) {
            view = v.findViewById(R.id.timetable_title)
            if (view.visibility == View.GONE) {
                view = v.findViewById(R.id.company_header)
            }
            desiredPeekHeight = if (view.visibility == View.GONE) {
                view.bottom + dp2px(12f)
            } else {
                view.top + view.height / 2
            }
        } else {
            desiredPeekHeight = view.bottom + dp2px(12f)
        }
        mapView?.let { mapView ->
            if (mapView.height - desiredPeekHeight < desiredPeekHeight * 0.7) {
                view = v.findViewById(R.id.image)
                desiredPeekHeight = view.bottom + dp2px(16f)
            }
        }
        bottomSheetBehavior.peekHeight = desiredPeekHeight
    }

    @SuppressLint("SetTextI18n")
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
        address.text = shop.street+ "\n" +  shop.zipCode + " " + shop.city
        address.contentDescription = getString(R.string.ShopDetails_Accessibility_address, shop.street, shop.zipCode, shop.city)
        phone.text = getString(R.string.ShopDetails_phone, shop.phone)
        phone.setClickDescription(R.string.ShopDetails_Accessibility_startCall)
        phone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", shop.phone, null))
            startActivity(intent)
        }

        val project = Snabble.projects.firstOrNull() { project -> project.shops.any { projectShop -> projectShop.id == shop.id } }
        project?.assets?.get("logo") { bm -> image.setImageBitmap(bm) }


        locationManager.location.observe(viewLifecycleOwner){ location ->
            distance.isVisible = location != null
            location?.let { currentLocation ->
                distance.visibility = View.VISIBLE
                val distanceString = currentLocation.toLatLng()
                    .distanceTo(LatLng(shop.latitude, shop.longitude))
                    .formatDistance()
                if (distance.text.toString() != distanceString) {
                    distance.text = distanceString
                    distance.contentDescription = getString(R.string.ShopDetails_Accessibility_distance, distanceString)
                }
            }
        }

        if (shop.openingHours.isNullOrEmpty()) {
            timetableTitle.visibility = View.GONE
        } else {
            timetable.removeAllViews()
            shop.openingHours?.forEach { spec ->
                val day = TextView(context)
                day.text = "${dayTable[spec.dayOfWeek]}: "
                timetable.addView(
                    day,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val textView = TextView(context)
                textView.text = spec.opens.take(5) + " \u2013 " + spec.closes.take(5)
                timetable.addView(
                    textView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }


        debugCheckin = view.findViewById(R.id.debug_checkin)
        val debug = isDebuggingAvailable
        val checkinAllowed = projectCode != null && project?.id == Snabble.projects[0].id
        if (checkinAllowed || debug) {
            debugCheckin.isVisible = true
            updateDebugCheckinText()
            debugCheckin.setOneShotClickListener {
                if (isCheckedInToShop) {
                    viewModel.checkOut()
                    Snabble.checkInManager.shop = null
                } else {
                    viewModel.checkIn()
                    Snabble.checkInManager.shop = shop
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

        val startScanner = view.findViewById<View>(R.id.start_scanner)
        startScanner.isVisible = isCheckedInToShop
        startScanner.setOneShotClickListener {
            //TODO : with(requireActivity()).push(AppSelfScanningFragment::class.java)
        }

        val company = project?.company
        companyCountry.setTextOrHide(project?.company?.country?.let(::getDisplayNameByIso3Code))
        companyName.setTextOrHide(project?.company?.name)
        companyStreet.setTextOrHide(project?.company?.street)
        if (company != null) {
            companyZip.text = company.zip + " " +company.city
        } else {
            companyHeader.visibility = View.GONE
            companyZip.visibility = View.GONE
        }
    }

    private val isCheckedInToShop: Boolean
        get() = Snabble.checkInManager.shop != null && Snabble.checkInManager.project != null && Snabble.checkInManager.shop?.id == shop.id

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

    private fun zoomToShop(shop: Shop, animate: Boolean) {
        mapPinHome.setImageResource(R.drawable.ic_map_home_active)
        homePinned = true
        zoomToLocation(LatLng(shop.latitude, shop.longitude), animate)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        // todo think of workaround
//        val toolbarId = requireActivity().getResourceId("R.id.toolbar")
//        val baseActivity = requireActivity() as AppCompatActivity
//        baseActivity.findViewById<Toolbar>(toolbarId).title = shop.name
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

//    override fun onCheckInStateChanged() {
//        updateDebugCheckinText()
//    }

    companion object {
        const val BUNDLE_KEY_SHOP = "shop"
        private const val BUNDLE_KEY_MAPVIEW = "mapView"
        const val KEY_MAPS_ENABLED = "mapsEnabled"
        private const val KEY_HIDDEN_MENU_AVAILABLE = "hiddenMenuAvailable"
        private const val KEY_DEBUGGING_AVAILABLE = "debuggingAvailable"
        const val KEY_PROJECT_CODE = "projectCode"

        fun View.setOneShotClickListener(callback: () -> Unit) =
            setOnClickListener(
                object : OneShotClickListener() {
                    override fun click() {
                        callback.invoke()
                    }
                }
            )

        fun isIntentAvailable(context: Context, intent: Intent): Boolean {
            val packageManager = context.packageManager
            val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list.isNotEmpty()
        }

        inline var View.behavior: CoordinatorLayout.Behavior<*>?
            get() = (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
            set(value) { (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = value }

        fun dp2px(dp: Float) = dp.dpInPx

        inline val Number.dpInPx: Int
            get() = dp.toInt()

        inline val Number.dp: Float
            get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), Resources.getSystem().displayMetrics)

        fun View.setClickDescription(stringId: Int, vararg formatArgs: Any) {
            setClickDescription(context.getString(stringId, formatArgs))
        }

    }
}