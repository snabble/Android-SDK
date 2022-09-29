package io.snabble.sdk.shopfinder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.shopfinder.shoplist.Item
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.setTextOrHide

class ShopListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
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

    fun bind(item: Item) {
        setClickDescription(R.string.Snabble_Shop_List_ShowDetails_accessibility)
        name.text = item.name
        address.text = item.address
        address.contentDescription = resources.getString(
            R.string.Snabble_Shop_Address_accessibility,
            item.street,
            item.zipCode,
            item.city
        )
        youAreHereContainer.isVisible = item.isCheckedIn

        val label = if (!item.isCheckedIn) item.distanceLabel else null
        distance.setTextOrHide(label)

        setOnClickListener {
            val shop = Snabble.getProjectById(item.project.id)?.shops?.first { it.id == item.id }
            SnabbleUiToolkit.executeAction(
                context,
                SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST,
                bundleOf("shop" to shop)
            )
        }
    }
}
