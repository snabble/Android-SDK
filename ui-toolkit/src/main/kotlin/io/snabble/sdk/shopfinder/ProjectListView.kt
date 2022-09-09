package io.snabble.sdk.shopfinder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import io.snabble.accessibility.accessibility
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.shopfinder.shoplist.Item
import io.snabble.sdk.shopfinder.shoplist.ViewType
import io.snabble.sdk.shopfinder.utils.AssetHelper
import io.snabble.sdk.shopfinder.utils.OneShotClickListener
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.setTextOrHide

class ProjectListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val itemAnimator = DefaultItemAnimator()
    private var image: ImageView
    private var name: TextView
    private var shopCount: TextView
    private var youAreHereIndicator: View
    private var distance: TextView
    private var chevron: ImageView

    init {
        inflate(context, R.layout.snabble_shop_item_list_group, this)

        image = findViewById(R.id.image)
        name = findViewById(R.id.name)
        shopCount = findViewById(R.id.shop_count)
        youAreHereIndicator = findViewById(R.id.you_are_here_indicator)
        distance = findViewById(R.id.distance)
        chevron = findViewById(R.id.chevron)
    }

    fun bind(item: Item, onToggle: () -> Unit) {
        val view = this
        name.text = item.name
        shopCount.text = context.resources.getQuantityString(
            R.plurals.Snabble_Shop_Finder_storeCount,
            item.shops!!, item.shops
        )
        val project = item.project
        image.setImageBitmap(null)
        AssetHelper.load(project.assets, "icon", image)

        if (item.type == ViewType.ExpandedBrand) {
            view.accessibility.setClickAction(R.string.Snabble_Shop_List_Colapse_accessibility)
            chevron.rotation = 270f
        } else {
            view.accessibility.setClickAction(R.string.Snabble_Shop_List_Expand_accessibility)
            chevron.rotation = 90f
        }

        view.setOnClickListener(object : OneShotClickListener() {
            override fun click() {
                onToggle()
                chevron.animate()
                    .rotationBy(if (item.type == ViewType.CollapsedBrand) -180f else 180f)
                    .setDuration(itemAnimator.addDuration)
                    .start()
                if (item.type == ViewType.ExpandedBrand) {
                    view.announceForAccessibility(
                        view.resources.getString(
                            R.string.Snabble_Shop_List_EventShopExpanded_accessibility,
                            item.name
                        )
                    )
                    view.accessibility.setClickAction(R.string.Snabble_Shop_List_Colapse_accessibility)
                } else {
                    view.accessibility.setClickAction(
                        view.resources.getString(
                            R.string.Snabble_Shop_List_EventShopColapsed_accessibility,
                            item.name
                        )
                    )
                    view.accessibility.setClickAction(R.string.Snabble_Shop_List_Expand_accessibility)
                }
            }
        })
        if (item.isCheckedIn) {
            youAreHereIndicator.isVisible = true
            distance.visibility = GONE
        } else {
            youAreHereIndicator.isVisible = false
            distance.setTextOrHide(item.distanceLabel)
            distance.setClickDescription(R.string.Snabble_Shop_Distance_accessibility)
        }
    }
}
