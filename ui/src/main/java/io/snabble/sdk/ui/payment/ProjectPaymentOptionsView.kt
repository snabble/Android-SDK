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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.*
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.payment.PaymentCredentialsStore
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.loadAsset
import java.util.*

open class ProjectPaymentOptionsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    companion object {
        const val ARG_BRAND = "brandId"
    }

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
        val dividerItemDecoration = DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.itemAnimator = null

        val listener = PaymentCredentialsStore.Callback {
            adapter.notifyDataSetChanged()
        }

        Snabble.getInstance().paymentCredentialsStore.addCallback(listener)

        getFragmentActivity()?.lifecycle?.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                adapter.notifyDataSetChanged()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                getFragmentActivity()?.lifecycle?.removeObserver(this)
                Snabble.getInstance().paymentCredentialsStore.removeCallback(listener)
            }
        })
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
        val count: TextView = itemView.findViewById(R.id.count)
        val image: ImageView = itemView.findViewById(R.id.helper_image)
    }

    inner class EntryAdapter : ListAdapter<Project, EntryViewHolder>(EntryItemDiffer()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return EntryViewHolder(inflater.inflate(R.layout.snabble_item_select_payment_project, parent, false))
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val project = getItem(position)
            holder.text.text = project.name
            holder.image.loadAsset(project.assets, "icon")

            val store = Snabble.getInstance().paymentCredentialsStore
            val credentials = store.allWithoutKeyStoreValidation

            val count = credentials.count {
                it.appId == Snabble.getInstance().config.appId &&
                it.type == PaymentCredentials.Type.CREDIT_CARD_PSD2 &&
                it.projectId == project.id
            }

            if (count > 0) {
                holder.count.visibility = View.VISIBLE
                holder.count.text = count.toString()
            } else {
                holder.count.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                if (count > 0) {
                    val args = Bundle()
                    args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE, PaymentCredentials.Type.CREDIT_CARD_PSD2)
                    args.putString(PaymentCredentialsListView.ARG_PROJECT_ID, project.id)
                    executeUiAction(SnabbleUI.Action.SHOW_PAYMENT_CREDENTIALS_LIST, args)
                } else {
                    val activity = UIUtils.getHostActivity(context)
                    if (activity is FragmentActivity) {
                        val dialogFragment = SelectPaymentMethodFragment()
                        val args = Bundle()
                        args.putSerializable(SelectPaymentMethodFragment.ARG_PAYMENT_METHOD_LIST, ArrayList(listOf(
                            PaymentMethod.VISA,
                            PaymentMethod.MASTERCARD,
                            PaymentMethod.AMEX))
                        )
                        args.putString(SelectPaymentMethodFragment.ARG_PROJECT_ID, project.id)
                        dialogFragment.arguments = args
                        dialogFragment.show(activity.supportFragmentManager, null)
                    } else {
                        throw RuntimeException("Host activity must be a FragmentActivity")
                    }
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