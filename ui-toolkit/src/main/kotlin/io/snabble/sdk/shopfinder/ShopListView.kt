package io.snabble.sdk.shopfinder

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.setTextOrHide

class ShopListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var name: TextView
    private var address: TextView
    private var distance: TextView
    private var youAreHereContainer: View

    init {
        inflate(context, R.layout.snabble_shop_item_list, this)

        name = findViewById(R.id.name)
        address = findViewById(R.id.address)
        distance = findViewById(R.id.distance)
        youAreHereContainer = findViewById(R.id.you_are_here_container)
    }

    fun bind(item: ExpandableShopListRecyclerView.Item) {
        val view = this
        view.setClickDescription(R.string.Snabble_Shop_List_ShowDetails_accessibility)
        name.text = item.name
        address.text = item.address
        address.contentDescription = view.resources.getString(
            R.string.Snabble_Shop_Address_accessibility,
            item.street,
            item.zipCode,
            item.city
        )
        youAreHereContainer.isVisible = item.isCheckedIn

        if (item.isCheckedIn) {
            distance.isVisible = false
        } else {
            distance.setTextOrHide(item.distanceLabel)
        }

        view.setOnClickListener {
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
