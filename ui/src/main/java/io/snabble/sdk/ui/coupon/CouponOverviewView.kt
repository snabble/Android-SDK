package io.snabble.sdk.ui.coupon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.dpInPx
import io.snabble.sdk.ui.utils.loadImage
import io.snabble.sdk.ui.utils.margin

class CouponOverviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.snabble_view_coupon_overview, this)
        findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = CouponsAdapter(requireNotNull(findViewTreeLifecycleOwner()))
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private class CouponsAdapter(
        lifecycleOwner: LifecycleOwner
    ) : ListAdapter<Coupon, CouponsHolder>(CouponDiffer()) {
        init {
            submitList(listOf(Coupon.createLoadingPlaceholder()))
            CouponManager.coupons.observe(lifecycleOwner) {
                updateList(it)
            }
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        fun updateList(model: List<Coupon>?) {
            if (model.isNullOrEmpty()) {
                submitList(listOf(Coupon.createEmptyPlaceholder()))
            } else {
                submitList(model)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CouponsHolder(parent)

        override fun onBindViewHolder(holder: CouponsHolder, position: Int) {
            val pos = when (position) {
                0 -> ItemPosition.First
                itemCount - 1 -> ItemPosition.Last
                else -> ItemPosition.Middle
            }
            holder.bind(pos, getItem(position))
        }
    }

    private class CouponDiffer : DiffUtil.ItemCallback<Coupon>() {
        override fun areItemsTheSame(oldItem: Coupon, newItem: Coupon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Coupon, newItem: Coupon) = oldItem == newItem
    }

    private enum class ItemPosition {
        First,
        Middle,
        Last
    }

    private class CouponsHolder(parent: ViewGroup) : RecyclerView.ViewHolder(inflate(parent)) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card)
        private val background: ImageView = itemView.findViewById(R.id.background)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val description: TextView = itemView.findViewById(R.id.description)
        private val discount: TextView = itemView.findViewById(R.id.discount)
        private val expire: TextView = itemView.findViewById(R.id.expire)
        private val loading: ProgressBar = itemView.findViewById(R.id.loading)
        private val nothing: TextView = itemView.findViewById(R.id.nothing)

        companion object {
            private fun inflate(parent: ViewGroup) =
                LayoutInflater.from(parent.context).inflate(R.layout.snabble_item_coupon, parent, false)
        }

        fun bind(position: ItemPosition, coupon: Coupon) {
            isLoading = coupon.mode == Coupon.Mode.Loading
            hasNoCoupons = coupon.mode == Coupon.Mode.Empty
            if (isLoading || hasNoCoupons) {
                cardView.setOnClickListener(null)
                itemView.margin.left = 32.dpInPx
                itemView.margin.right = 32.dpInPx
                itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                return
            }
            cardView.setCardBackgroundColor(coupon.backgroundColor)
            background.loadImage(coupon.imageURL)
            title.text = coupon.title
            title.setTextColor(coupon.textColor)
            description.text = coupon.subtitle
            description.setTextColor(coupon.textColor)
            discount.text = coupon.text
            discount.setTextColor(coupon.textColor)
            expire.text = coupon.buildExpireString(discount.resources)
            expire.setTextColor(coupon.textColor)
            itemView.margin.left = if (position == ItemPosition.First) 32.dpInPx else 12.dpInPx
            itemView.margin.right = if (position == ItemPosition.Last) 32.dpInPx else 12.dpInPx
            itemView.layoutParams.width =
                (itemView.context.resources.displayMetrics.widthPixels * 0.7).toInt()

            cardView.setOnClickListener {
                TODO("Add the logic here")
                //SnabbleUI.executeAction(it.context, TODO) // TODO also add activity
            }
        }

        var isLoading: Boolean = false
            set(value) {
                field = value
                background.isInvisible = value
                title.isInvisible = value
                description.isInvisible = value
                discount.isInvisible = value
                expire.isInvisible = value
                loading.isVisible = value
            }

        var hasNoCoupons: Boolean = true
            set(value) {
                field = value
                background.isInvisible = value
                title.isInvisible = value
                description.isInvisible = value
                discount.isInvisible = value
                expire.isInvisible = value
                nothing.isVisible = value
            }
    }
}