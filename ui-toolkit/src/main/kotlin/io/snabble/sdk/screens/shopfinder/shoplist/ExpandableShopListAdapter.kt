package io.snabble.sdk.screens.shopfinder.shoplist

import android.annotation.SuppressLint
import android.location.Location
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Snabble
import io.snabble.sdk.screens.shopfinder.ProjectListView
import io.snabble.sdk.screens.shopfinder.ShopListView

internal class ExpandableShopListAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(
    ShopDiffer()
) {

    var currentShopId: String? = null
    var showAll: Boolean = false
    val expandedProjects = mutableListOf<String>()

    private var items = emptyList<Item>()
    private var lastKnownLocation: Location? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_SECTION -> ShopSectionViewHolder(ProjectListView(parent.context))
        else -> ShopViewHolder(ShopListView(parent.context))
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position).type) {
            ViewType.ExpandedBrand,
            ViewType.CollapsedBrand -> TYPE_SECTION
            ViewType.HiddenShop, // can never happen should be always filtered out
            ViewType.VisibleShop -> TYPE_SHOP
        }

    override fun getItemId(position: Int) = getItem(position).id.hashCode().toLong()

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
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
            TYPE_SHOP -> (vh as ShopViewHolder).bindTo(model)
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
            items = items
                .groupBy { it.project.name }
                .map { (_, shops) ->
                    // Set distance for brand to -1 to keep it at top
                    val brand = shops.first { it.type.isBrand }
                    brand.distance = -1f
                    val sorted = shops.sortedBy { it.distance }
                    // apply shortest distance to the brand row
                    brand.distance = sorted.first { it.type.isShop }.distance
                    sorted
                }
                .sortedBy { values -> values.first().distance }
                .flatten()
        }

        applyVisibility(items)
    }

    fun addCheckInObserverForDistanceUpdate(lifecycleOwner: LifecycleOwner) {
        Snabble.currentCheckedInShop.observe(lifecycleOwner) { shop ->
            currentShopId = shop?.id
            updateDistances(lastKnownLocation)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
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
            notifyDataSetChanged()
            return
        }

        submitList(model.filterNot { it.type == ViewType.HiddenShop })
        notifyDataSetChanged()
    }

    companion object {

        private const val TYPE_SECTION = 0
        private const val TYPE_SHOP = 1
    }

    private class ShopSectionViewHolder(itemView: ProjectListView) :
        RecyclerView.ViewHolder(itemView) {

        fun bindTo(item: Item, onToggle: () -> Unit) {
            (itemView as ProjectListView).bind(item, onToggle)
        }
    }

    private class ShopViewHolder(itemView: ShopListView) : RecyclerView.ViewHolder(itemView) {

        fun bindTo(item: Item) {
            (itemView as ShopListView).bind(item)
        }
    }

    private class ShopDiffer : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(
            oldItem: Item,
            newItem: Item
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Item,
            newItem: Item
        ) = oldItem == newItem
    }
}
