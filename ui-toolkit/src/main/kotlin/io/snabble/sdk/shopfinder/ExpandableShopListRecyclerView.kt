package io.snabble.sdk.shopfinder

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.customview.view.AbsSavedState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.*
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.shopfinder.utils.ConfigurableDivider
import io.snabble.sdk.shopfinder.utils.formatDistance
import io.snabble.sdk.ui.utils.UIUtils.getHostActivity
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks


/**
 * Recycler view for projects and shops.
 * If there is more than one project given it inflates an expandable view, showing
 * the projects as top layer. With a click on a specific projects
 * the corresponding shops are shown right below.
 *
 * If the user is checked into a specific shop, the corresponding project is marked with a dot
 * and the shop is marked with a "you are here" icon.
 * For the check in marks to work 'Snabble' checkedInShop and checkedInProject properties need
 * to be set according to the current state.
 */
class ExpandableShopListRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle), Snabble.OnMetadataUpdateListener {

    private val adapter = ExpandableShopListAdapter()
    private val layoutManager = LinearLayoutManager(context)
    private val itemAnimator = DefaultItemAnimator()
    private var chosenProjects: List<Project>? = null

    /**
     * If set to true, all the shops of a project are displayed as list directly,
     * without the project top layer.
     *
     * This is needed if u pass just one project to the view but the application holds more
     * than one project.
     * */
    @Suppress("unused")
    var showAll: Boolean
        get() = adapter.showAll
        set(value) {
            adapter.showAll = value
        }

    init {
        setAdapter(adapter)
        setLayoutManager(layoutManager)
        itemAnimator.supportsChangeAnimations = false
        setItemAnimator(itemAnimator)
        clipChildren = false
        clipToPadding = false
        val dividerItemDecoration = ConfigurableDivider(
            context,
            DividerItemDecoration.VERTICAL,
            skipLastDivider = true,
            justBetweenSameType = false
        )
        addItemDecoration(dividerItemDecoration)
    }

    /**
     * Takes a list of projects and converts it list consisting of
     * projects and corresponding shops for internal use.
     */
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

    /**
     * Takes a location as parameter and updates the view
     * with the new distance to the given location
     */
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
        adapter.lifecycleOwner = findViewTreeLifecycleOwner()
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

    /**
     * Update and refresh the shown shops.
     */
    fun update() {
        chosenProjects?.let(::setShopsByProjects)
    }

    class SavedState(superState: Parcelable) : AbsSavedState(superState) {
        var expanded: Array<String> = emptyArray()

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(superState, flags)
            out.writeStringArray(expanded)
        }

    }

    // Inflates Item shop group Layout
    // e.g. preview shows projects as list with numbers of shops available
    private class ShopSectionViewHolder(itemView: ProjectListView) : ViewHolder(itemView) {

        fun bindTo(item: Item, onToggle: () -> Unit) {
            (itemView as ProjectListView).bind(item, onToggle)
        }
    }

    // Inflates list shop layout
    // e.g. sublist of expandable view. shows all shops
    private class ShopViewHolder(itemView: ShopListView) : ViewHolder(itemView) {

        fun bindTo(item: Item) {
            (itemView as ShopListView).bind(item)
        }
    }

    private class ShopDiffer : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }

    private class ExpandableShopListAdapter : ListAdapter<Item, ViewHolder>(
        ShopDiffer()
    ) {
        var currentShopId: String? = null

        var lifecycleOwner: LifecycleOwner? = null
            set(value) {
                field = value
                Snabble.currentCheckedInShop.observe(requireNotNull(value)) { shop ->
                    currentShopId = shop?.id
                    /**If the user walks into the shop trigger refresh*/
                    updateDistances(lastKnownLocation)
                }
            }
        var showAll: Boolean = false
        val expandedProjects = mutableListOf<String>()

        private var items = emptyList<Item>()
        private var lastKnownLocation: Location? = null

        private val TYPE_SECTION = 0
        private val TYPE_SHOP = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            TYPE_SECTION -> ShopSectionViewHolder(
                ProjectListView(parent.context)
            )
            else -> ShopViewHolder(
                ShopListView(parent.context)
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
            val currentProjectId = Snabble.checkedInProject.value?.id

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

            if (Snabble.projects.size == 1 || showAll) {
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