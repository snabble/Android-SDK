package io.snabble.sdk.shopfinder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.customview.view.AbsSavedState
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import io.snabble.accessibility.accessibility
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.location.formatDistance
import io.snabble.sdk.shopfinder.ShopListFragment.Companion.shopsOnly
import io.snabble.sdk.shopfinder.utils.AssetHelper.load
import io.snabble.sdk.shopfinder.utils.ConfigurableDivider
import io.snabble.sdk.shopfinder.utils.OneShotClickListener
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.UIUtils.getHostActivity
import io.snabble.sdk.ui.utils.getString
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks
import io.snabble.sdk.utils.setTextOrHide

class ExpandableShopListRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle), Snabble.OnMetadataUpdateListener {

    private var viewmodel: ShopfinderViewModel

    private val adapter = ExpandableShopListAdapter(context)
    private val layoutManager = LinearLayoutManager(context)
    private val itemAnimator = DefaultItemAnimator()
    private var chosenProjects: List<Project>? = null

    var showAll: Boolean
        get() = adapter.showAll
        set(value) {
            adapter.showAll = value
        }

    init {
        viewmodel = ViewModelProvider(context as AppCompatActivity)[ShopfinderViewModel::class.java]
        setAdapter(adapter)
        setLayoutManager(layoutManager)
        itemAnimator.supportsChangeAnimations = false
        setItemAnimator(itemAnimator)
        clipChildren = false
        clipToPadding = false
        val dividerItemDecoration = ConfigurableDivider(
            context,
            DividerItemDecoration.VERTICAL,
            true,
            false
        )
        addItemDecoration(dividerItemDecoration)
    }

    fun setShopsByProjects(projects: List<Project>) {
        chosenProjects = projects
        val model = mutableListOf<Item>()
        projects.filter {
            it.shops.isNotEmpty()
        }.forEach { project ->
            model += Item(project)
            project.shops.forEach { shop ->
                model += Item(shop, project)
            }
        }
        adapter.updateModel(model)
    }

    fun sortByDistance(location: Location?) {
        adapter.updateDistances(location)
    }

    override fun onSaveInstanceState(): Parcelable {
        val ss = SavedState(requireNotNull(super.onSaveInstanceState()))
        ss.expanded = adapter.expandedProjects.toTypedArray()
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        adapter.expandedProjects.addAll(state.expanded)
    }

    private fun registerListeners() {
        Snabble.addOnMetadataUpdateListener(this)
    }

    private fun unregisterListeners() {
        Snabble.removeOnMetadataUpdateListener(this)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            val application = context.applicationContext as Application
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            registerListeners()
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        unregisterListeners()
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                if (getHostActivity(context) === activity) {
                    registerListeners()
                    chosenProjects?.let(::setShopsByProjects)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (getHostActivity(context) === activity) {
                    unregisterListeners()
                }
            }
        }

    override fun onMetaDataUpdated() {
        update()
    }

    fun update() {
        chosenProjects?.let(::setShopsByProjects)
    }

    class SavedState : AbsSavedState {
        var expanded: Array<String> = emptyArray()

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(superState, flags)
            out.writeStringArray(expanded)
        }

        @SuppressLint("RestrictedApi")
        private constructor(`in`: Parcel) : super(requireNotNull(`in`.readParcelable(RecyclerView.SavedState::class.java.classLoader))) {
            expanded = `in`.createStringArray() ?: emptyArray()
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(`in`: Parcel) = SavedState(`in`)
                    override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
                }
        }
    }

    // Inflates Item shop group Layout
    // e.g. preview shows projects as list with numbers of shops available
    private class ShopSectionViewHolder(itemView: View) : ViewHolder(itemView) {
        private val itemAnimator = DefaultItemAnimator()
        var image: ImageView = itemView.findViewById(R.id.image)
        var name: TextView = itemView.findViewById(R.id.name)
        var shopCount: TextView = itemView.findViewById(R.id.shop_count)
        var youAreHereIndicator: View = itemView.findViewById(R.id.you_are_here_indicator)
        var distance: TextView = itemView.findViewById(R.id.distance)
        var chevron: ImageView = itemView.findViewById(R.id.chevron)


        fun bindTo(item: Item, onToggle: () -> Unit) {
            name.text = item.name
            shopCount.text = itemView.context.resources.getQuantityString(
                R.plurals.ShopList_numberOfStores,
                item.shops!!, item.shops
            )
            val project = item.project
            image.setImageBitmap(null)
            load(project.assets, "icon", image)

            if (item.type == ViewType.ExpandedBrand) {
                itemView.accessibility.setClickAction(R.string.ShopList_Accessibility_colapse)
                chevron.rotation = 270f
            } else {
                itemView.accessibility.setClickAction(R.string.ShopList_Accessibility_expand)
                chevron.rotation = 90f
            }

            itemView.setOnClickListener(object : OneShotClickListener() {
                override fun click() {
                    onToggle()
                    chevron.animate()
                        .rotationBy(if (item.type == ViewType.CollapsedBrand) -180f else 180f)
                        .setDuration(itemAnimator.addDuration)
                        .start()
                    if (item.type == ViewType.ExpandedBrand) {
                        itemView.announceForAccessibility(
                            getString(
                                R.string.ShopList_Accessibility_eventShopExpanded,
                                item.name
                            )
                        )
                        itemView.accessibility.setClickAction(R.string.ShopList_Accessibility_colapse)
                    } else {
                        itemView.accessibility.setClickAction(
                            getString(
                                R.string.ShopList_Accessibility_eventShopColapsed,
                                item.name
                            )
                        )
                        itemView.accessibility.setClickAction(R.string.ShopList_Accessibility_expand)
                    }
                }
            })
            if (item.isCheckedIn) {
                youAreHereIndicator.visibility = VISIBLE
                distance.visibility = GONE
            } else {
                youAreHereIndicator.visibility = GONE
                distance.setTextOrHide(item.distanceLabel)
            }
        }
    }

    // Inflates list shop layout
    // e.g. sublist of expandable view. shows all shops
    private class ShopViewHolder(itemView: View, var context: Context) : ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.name)
        var address: TextView = itemView.findViewById(R.id.address)
        var distance: TextView = itemView.findViewById(R.id.distance)
        var youAreHereContainer: View = itemView.findViewById(R.id.you_are_here_container)

        fun bindTo(item: Item) {
            name.text = item.name
            address.text = item.address
            address.contentDescription = getString(
                R.string.ShopList_Accessibility_address,
                item.street,
                item.zipCode,
                item.city
            )
            youAreHereContainer.isVisible = item.isCheckedIn

            if (item.isCheckedIn) {
                distance.visibility = GONE
            } else {
                distance.setTextOrHide(item.distanceLabel)
            }

            itemView.setOnClickListener {
                val args = Bundle()
                val shop =
                    Snabble.getProjectById(item.project.id)?.shops?.first { it.id == item.id }
                args.putParcelable("shop", shop)

                SnabbleUiToolkit.executeAction(
                    context,
                    SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST,
                    args
                )
            }
        }
    }

    private class ShopDiffer : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }

    private class ExpandableShopListAdapter(var context: Context) : ListAdapter<Item, ViewHolder>(
        ShopDiffer()
    ) {

        var showAll: Boolean = false
        val expandedProjects = mutableListOf<String>()

        private var items = emptyList<Item>()
        private var lastKnownLocation: Location? = null

        private val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        private val TYPE_SECTION = 0
        private val TYPE_SHOP = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            TYPE_SECTION -> ShopSectionViewHolder(
                layoutInflater.inflate(
                    R.layout.snabble_shop_item_list_group,
                    parent,
                    false
                )
            )
            else -> ShopViewHolder(
                layoutInflater.inflate(
                    R.layout.snabble_shop_item_list,
                    parent,
                    false
                ), context
            )
        }


        override fun getItemViewType(position: Int): Int =
            when (getItem(position).type) {
                ViewType.ExpandedBrand,
                ViewType.CollapsedBrand -> TYPE_SECTION
                ViewType.HiddenShop, // can never happen should be always filtered out
                ViewType.VisibleShop -> TYPE_SHOP
            }

        override fun getItemId(position: Int) = getItem(position).id.hashCode().toLong()

        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            val model = getItem(position)
            when (getItemViewType(position)) {
                TYPE_SECTION -> {
                    (vh as ShopSectionViewHolder).bindTo(model) {
                        if (expandedProjects.contains(model.project.id)) {
                            expandedProjects.remove(model.project.id)
                        } else {
                            expandedProjects.add(model.project.id)
                        }
                        applyVisibility(items)
                    }
                }
                TYPE_SHOP -> {
                    (vh as ShopViewHolder).bindTo(model)
                }
            }
        }

        fun updateModel(model: List<Item>) {
            items = model
            updateDistances(lastKnownLocation)
        }

        fun updateDistances(location: Location?) {
            lastKnownLocation = location
            if (location != null) {
                // update distances on each shop
                items.forEach {
                    if (it.type.isShop) {
                        it.updateDistance(location)
                    }
                }

                // Sort shops brand wise
                items = items.groupBy {
                    it.project.name
                }.map { (_, shops) ->
                    // Set distance for brand to -1 to keep it at top
                    val brand = shops.first { it.type.isBrand }
                    brand.distance = -1f
                    val sorted = shops.sortedBy { it.distance }
                    // apply shortest distance to the brand row
                    brand.distance = sorted.first { it.type.isShop }.distance
                    sorted
                }.sortedBy { values ->
                    values.first().distance
                }.flatten()
            }

            applyVisibility(items)
        }

        private fun applyVisibility(model: List<Item>) {
            if (showAll) {
                submitList(model.filter { it.type.isShop })
                return
            }

            var currentProjectId: String? = null
            val currentShopId = Snabble.checkedInShop?.id

            (getHostActivity(context) as AppCompatActivity).let { app ->
                Snabble.checkedInProject.observe(app) {
                    currentProjectId = it?.id
                }
            }

            model.forEach { item ->
                when (item.type) {
                    ViewType.ExpandedBrand,
                    ViewType.CollapsedBrand -> {
                        item.isCheckedIn =
                            currentProjectId != null && item.project.id == currentProjectId
                        item.type = if (expandedProjects.contains(item.project.id)) {
                            ViewType.ExpandedBrand
                        } else {
                            ViewType.CollapsedBrand
                        }
                    }
                    ViewType.HiddenShop,
                    ViewType.VisibleShop -> {
                        item.isCheckedIn = item.id == currentShopId
                        item.type = if (expandedProjects.contains(item.project.id)) {
                            ViewType.VisibleShop
                        } else {
                            ViewType.HiddenShop
                        }
                    }
                }
            }

            if (shopsOnly) {
                submitList(model.filter { it.type.isShop })
                return
            }

            submitList(model.filterNot { it.type == ViewType.HiddenShop })
        }
    }

    enum class ViewType(val isShop: Boolean) {
        CollapsedBrand(false),
        ExpandedBrand(false),
        HiddenShop(true),
        VisibleShop(true);

        val isBrand: Boolean
            get() = !isShop
    }

    data class Item(
        val id: String,
        val project: Project,
        var location: Location,
        val name: String,
        var type: ViewType,
        var isCheckedIn: Boolean = false,
        val shops: Int? = null, // null for Shops
        val street: String? = null, // null for a SectionHeader
        val zipCode: String? = null, // null for a SectionHeader
        val city: String? = null, // null for a SectionHeader
        var distance: Float? = null // null without location
    ) : Comparable<Item> {
        constructor(project: Project) : this(
            id = project.id,
            project = project,
            location = project.shops.first().location,
            name = project.brand?.name ?: project.name,
            type = ViewType.CollapsedBrand,
            shops = project.shops.size
        )

        constructor(shop: Shop, project: Project) : this(
            id = shop.id,
            project = project,
            location = shop.location,
            name = shop.name,
            type = ViewType.HiddenShop,
            street = shop.street,
            zipCode = shop.zipCode,
            city = shop.city
        )

        val address: String?
            get() =
                if (street != null && city != null) {
                    street + "\n" + zipCode.orEmpty() + " " + city
                } else null

        val distanceLabel: String?
            get() = distance?.formatDistance()

        fun updateDistance(to: Location) =
            location.distanceTo(to).also {
                distance = it
            }

        override fun compareTo(other: Item): Int = distance?.compareTo(other.distance ?: 0f) ?: 0
    }

}