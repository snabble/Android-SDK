package io.snabble.sdk.ui.coupon

import android.content.Context
import android.os.Bundle
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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
import io.snabble.sdk.ui.utils.padding

/**
 * Show the coupons for the given [LiveData], set via [setCouponSource]. The padding left and right is overwritten so
 * that you can apply your own key lines so that the view can use the full width to display the coupon cards.
 * You can control with [hideWhenLoading] and [hideWhenEmpty] if the view should be shown in that states.
 */
class CouponOverviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var adapter: CouponsAdapter? = null
    private val recyclerView: RecyclerView
    private var keyLineLeft: Int
    private var keyLineRight: Int
    var isInEmptyState = true
        private set

    init {
        keyLineLeft = padding.left
        keyLineRight = padding.right
        if (!isInEditMode) {
            super.setPadding(0, paddingTop, 0, paddingBottom)
        }
        inflate(context, R.layout.snabble_view_coupon_overview, this)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    fun interface EmptyStageChangeListener {
        fun onEmptyStageChanged(isEmpty: Boolean)
    }

    fun addEmptyStateListener(listener: EmptyStageChangeListener) {
        ensureAdapterExists().emptyStateListener += listener
    }

    fun removeEmptyStateListener(listener: EmptyStageChangeListener) {
        ensureAdapterExists().emptyStateListener -= listener
    }

    fun setCouponSource(coupons: LiveData<List<Coupon>>) {
        recyclerView.adapter = ensureAdapterExists().apply {
            setCouponSource(coupons)
        }
    }

    private fun ensureAdapterExists(): CouponsAdapter {
        if (adapter == null) {
            adapter = CouponsAdapter(requireNotNull(findViewTreeLifecycleOwner()), keyLineLeft, keyLineRight).apply {
                emptyStateListener += EmptyStageChangeListener { isEmpty ->
                    isInEmptyState = isEmpty
                }
            }
        }
        return adapter!!
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, top, 0, bottom)
        keyLineLeft = left
        keyLineRight = right
    }

    private class CouponsAdapter(
        private val lifecycleOwner: LifecycleOwner,
        private val paddingLeft: Int,
        private val paddingRight: Int
    ) : ListAdapter<Coupon, CouponsHolder>(CouponDiffer()), Observer<List<Coupon>> {
        private var coupons: LiveData<List<Coupon>>? = null
        private var wasEmpty: Boolean? = null
        val emptyStateListener = mutableListOf<EmptyStageChangeListener>()

        init {
            submitList(listOf(Coupon.createLoadingPlaceholder()))
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        fun setCouponSource(coupons: LiveData<List<Coupon>>) {
            this.coupons?.removeObserver(this)
            this.coupons = coupons
            coupons.observe(lifecycleOwner, this)
        }

        override fun onChanged(coupons: List<Coupon>?) {
            updateList(coupons)
        }

        fun updateList(model: List<Coupon>?) {
            if (model.isNullOrEmpty()) {
                submitList(listOf(Coupon.createEmptyPlaceholder()))
                if (wasEmpty != true) {
                    emptyStateListener.forEach { it.onEmptyStageChanged(true) }
                }
            } else {
                submitList(model)
                if (wasEmpty != false) {
                    emptyStateListener.forEach { it.onEmptyStageChanged(false) }
                }
            }
            wasEmpty = model.isNullOrEmpty()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CouponsHolder(parent, paddingLeft, paddingRight)

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

    private class CouponsHolder(parent: ViewGroup, private val paddingLeft: Int, private val paddingRight: Int) : RecyclerView.ViewHolder(inflate(parent)) {
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
                itemView.margin.left = paddingLeft
                itemView.margin.right = paddingRight
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
            itemView.margin.left = if (position == ItemPosition.First) paddingLeft else 12.dpInPx
            itemView.margin.right = if (position == ItemPosition.Last) paddingRight else 12.dpInPx
            itemView.layoutParams.width = (itemView.context.resources.displayMetrics.widthPixels * 0.7).toInt()

            cardView.setOnClickListener {
                val args = Bundle()
                args.putParcelable(CouponDetailActivity.ARG_COUPON, coupon)
                SnabbleUI.executeAction(it.context, SnabbleUI.Event.SHOW_COUPON_DETAILS, args)
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