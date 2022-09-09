package io.snabble.sdk.shopfinder.shoplist

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.customview.view.AbsSavedState
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.shopfinder.utils.ConfigurableDivider
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
) : RecyclerView(context, attrs, defStyle),
    Snabble.OnMetadataUpdateListener {

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
        val model = projects
            .filter { it.shops.isNotEmpty() }
            .flatMap { project ->
                mutableListOf<Item>().apply {
                    add(Item(project))
                    project.shops.forEach { shop -> add(Item(shop, project)) }
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
        val state = SavedState(requireNotNull(super.onSaveInstanceState()))
        state.expanded = adapter.expandedProjects.toTypedArray()
        return state
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
        findViewTreeLifecycleOwner()?.let(adapter::addCheckInObserverForDistanceUpdate)
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
}

