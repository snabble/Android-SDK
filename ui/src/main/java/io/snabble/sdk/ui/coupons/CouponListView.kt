package io.snabble.sdk.ui.coupons

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Coupon
import io.snabble.sdk.CouponUpdateCallback
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI

class CouponListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val adapter: CouponAdapter
    private val recylerView: RecyclerView

    init {
        inflate(context, R.layout.snabble_view_coupons, this)

        adapter =  CouponAdapter()
        recylerView = findViewById(R.id.recycler_view)
        recylerView.adapter = adapter
        recylerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        SnabbleUI.getProject().couponApi.getAsync(object : CouponUpdateCallback {
            override fun success(coupons: List<Coupon>) {
                adapter.submitList(coupons)
            }

            override fun failure() {

            }
        })
    }

    class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.name)
        val code = itemView.findViewById<TextView>(R.id.code)
    }

    inner class CouponAdapter : ListAdapter<Coupon, CouponViewHolder>(CouponDiffer()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
            return CouponViewHolder(View.inflate(context, R.layout.snabble_item_coupon, parent))
        }

        override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.name
            holder.code.text = item.codes.getOrNull(0)?.code ?: "null"
        }
    }
}


class CouponDiffer : DiffUtil.ItemCallback<Coupon>() {
    override fun areItemsTheSame(oldItem: Coupon, newItem: Coupon) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Coupon, newItem: Coupon) = oldItem == newItem
}