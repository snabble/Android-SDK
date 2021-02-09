package io.snabble.sdk.ui.payment

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.loadAsset
import java.util.*

class SelectProjectPaymentView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var adapter: EntryAdapter

    var projects: List<Project>? = null
        set(value) {
            field = value
            adapter.submitList(value)
        }

    init {
        inflate(context, R.layout.snabble_select_payment_project, this)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        adapter = EntryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
        val image: ImageView = itemView.findViewById(R.id.helper_image)
    }

    inner class EntryAdapter : ListAdapter<Project, EntryViewHolder>(EntryItemDiffer()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return EntryViewHolder(inflater.inflate(R.layout.snabble_item_select_payment_project, parent, false))
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val entry = getItem(position)
            holder.text.text = entry.name
            holder.image.loadAsset(entry.assets, "icon")
            holder.itemView.setOnClickListener {
                val activity = UIUtils.getHostActivity(context)
                if (activity is FragmentActivity) {
                    val dialogFragment = SelectPaymentMethodFragment()
                    val args = Bundle()
                    args.putSerializable(SelectPaymentMethodFragment.ARG_PAYMENT_METHOD_LIST, ArrayList(listOf(
                        PaymentMethod.VISA,
                        PaymentMethod.MASTERCARD,
                        PaymentMethod.AMEX))
                    )
                    args.putString(SelectPaymentMethodFragment.ARG_PROJECT_ID, entry.id)
                    dialogFragment.arguments = args
                    dialogFragment.show(activity.supportFragmentManager, null)
                } else {
                    throw RuntimeException("Host activity needs to be a FragmentActivity")
                }
            }
        }
    }

    class EntryItemDiffer : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
            return oldItem.id == newItem.id
        }
    }
}