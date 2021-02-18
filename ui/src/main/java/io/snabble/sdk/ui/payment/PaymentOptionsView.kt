package io.snabble.sdk.ui.payment

import android.content.Context
import android.graphics.Rect
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
import io.snabble.sdk.Brand
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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class PaymentOptionsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.snabble_payment_options, this)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val adapter = EntryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.itemAnimator = null
        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)
        adapter.submitList(getEntries())

        val listener = PaymentCredentialsStore.OnPaymentCredentialsAddedListener {
            adapter.notifyDataSetChanged()
        }

        getFragmentActivity()?.lifecycle?.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                adapter.notifyDataSetChanged()
                Snabble.getInstance().paymentCredentialsStore.addOnPaymentCredentialsAddedListener(listener)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                getFragmentActivity()?.lifecycle?.removeObserver(this)
                Snabble.getInstance().paymentCredentialsStore.removeOnPaymentCredentialsAddedListener(listener)
            }
        })
    }

    private fun getEntries(): List<Entry> {
        val paymentMethods = paymentMethods()
        val projectsWithCreditCards = projectsWithCreditCards()
        val globalList = ArrayList<Entry>()
        val projectList = ArrayList<Entry>()

        globalList.add(
            Entry(
                isSectionHeader = true,
                text = resources.getString(R.string.Snabble_PaymentMethods_forAllRetailers),
            )
        )

        val store = Snabble.getInstance().paymentCredentialsStore
        val credentials = store.allWithoutKeyStoreValidation

        if (paymentMethods.contains(PaymentMethod.DE_DIRECT_DEBIT)) {
            val count = credentials.count {
                it.appId == Snabble.getInstance().config.appId &&
                it.type == PaymentCredentials.Type.SEPA
            }

            globalList.add(
                Entry(
                    text = resources.getString(R.string.Snabble_Payment_SEPA_Title),
                    icon = R.drawable.snabble_ic_payment_select_sepa,
                    count = count,
                    click = {
                        if (count > 0) {
                            val args = Bundle()
                            args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE, PaymentCredentials.Type.SEPA)
                            executeUiAction(SnabbleUI.Action.SHOW_PAYMENT_CREDENTIALS_LIST, args)
                        } else {
                            executeUiAction(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT)
                        }
                    }
                )
            )
        }

        if (paymentMethods.contains(PaymentMethod.PAYDIREKT)) {
            val count = credentials.count {
                it.appId == Snabble.getInstance().config.appId &&
                it.type == PaymentCredentials.Type.PAYDIREKT
            }

            globalList.add(
                Entry(
                    text = "Paydirekt",
                    icon = R.drawable.snabble_ic_payment_select_paydirekt,
                    count = count,
                    click = {
                        if (count > 0) {
                            val args = Bundle()
                            args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE, PaymentCredentials.Type.PAYDIREKT)
                            executeUiAction(SnabbleUI.Action.SHOW_PAYMENT_CREDENTIALS_LIST, args)
                        } else {
                            executeUiAction(SnabbleUI.Action.SHOW_PAYDIREKT_INPUT)
                        }
                    }
                )
            )
        }

        projectList.add(
            Entry(
                isSectionHeader = true,
                text = resources.getString(R.string.Snabble_PaymentMethods_forSingleRetailer),
            )
        )

        projectsWithCreditCards
            .filter { it.brand == null }
            .forEach { project ->
                val count = credentials.count {
                    it.appId == Snabble.getInstance().config.appId &&
                    it.type == PaymentCredentials.Type.CREDIT_CARD_PSD2 &&
                    it.projectId == project.id
                }

                projectList.add(
                    Entry(
                        text = project.name,
                        project = project,
                        count = count,
                        click = {
                            if (count > 0) {
                                val args = Bundle()
                                args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE, PaymentCredentials.Type.CREDIT_CARD_PSD2)
                                args.putSerializable(PaymentCredentialsListView.ARG_PROJECT_ID, project.id)
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
                    )
                )
        }

        val brands = ArrayList<Brand>()
        val counts = HashMap<Brand, Int>()

        projectsWithCreditCards
            .filter { it.brand != null }
            .forEach { project ->
                val count = credentials.count {
                    it.appId == Snabble.getInstance().config.appId
                    && it.type == PaymentCredentials.Type.CREDIT_CARD_PSD2
                    && it.projectId == project.id }

                if (!brands.contains(project.brand)) {
                    brands.add(project.brand)
                }

                val currentCount = counts.getOrPut(project.brand) { 0 }
                counts[project.brand] = currentCount + count
        }

        brands.forEach { brand ->
            projectList.add(
                Entry(
                    text = brand.name,
                    project = projectsWithCreditCards.firstOrNull { it.brand.id == brand.id },
                    count = counts[brand] ?: 0,
                    click = {
                        val args = Bundle()
                        args.putString(ProjectPaymentOptionsView.ARG_BRAND, brand.id)
                        executeUiAction(SnabbleUI.Action.SHOW_PROJECT_PAYMENT_OPTIONS, args)
                    }
                )
            )
        }

        val adapterList = ArrayList<Entry>()

        if (globalList.size > 1) {
            adapterList.addAll(globalList)
        }

        if (projectList.size > 1) {
            adapterList.addAll(projectList)
        }

        return adapterList
    }

    private fun paymentMethods(): Set<PaymentMethod> {
        val availablePaymentMethods = HashSet<PaymentMethod>()
        Snabble.getInstance().projects.forEach {
            availablePaymentMethods.addAll(it.availablePaymentMethods)
        }
        return availablePaymentMethods
    }

    private fun projectsWithCreditCards(): List<Project> {
        return Snabble.getInstance().projects.filter { project ->
            project.availablePaymentMethods.any {
                    it == PaymentMethod.VISA
                 || it == PaymentMethod.MASTERCARD
                 || it == PaymentMethod.AMEX }
        }
    }

    data class Entry(
        val isSectionHeader: Boolean = false,
        val text: String,
        val count: Int = 0,
        val icon: Int? = null,
        val project: Project? = null,
        val click: OnClickListener? = null,
    )

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
        val count: TextView = itemView.findViewById(R.id.count)
        val image: ImageView = itemView.findViewById(R.id.helper_image)
    }

    class EntryAdapter : ListAdapter<Entry, RecyclerView.ViewHolder>(EntryItemDiffer()) {
        companion object {
            const val SECTION_HEADER = 0
            const val ENTRY = 1
        }
        override fun getItemViewType(position: Int): Int {
            return if (getItem(position).isSectionHeader) SECTION_HEADER else ENTRY
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when(viewType) {
                SECTION_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.snabble_item_payment_options_header, parent, false))
                else -> EntryViewHolder(inflater.inflate(R.layout.snabble_item_payment_options_entry, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val entry = getItem(position)
            if (holder is HeaderViewHolder) {
                holder.text.text = entry.text
            } else if (holder is EntryViewHolder){
                holder.text.text = entry.text

                if (entry.icon != null) {
                    holder.image.setImageResource(entry.icon)
                }

                if (entry.project != null) {
                    holder.image.loadAsset(entry.project.assets, "icon")
                }

                if (entry.count > 0) {
                    holder.count.visibility = View.VISIBLE
                    holder.count.text = entry.count.toString()
                } else {
                    holder.count.visibility = View.GONE
                }

                holder.itemView.setOnClickListener(entry.click)
            }
        }
    }

    class EntryItemDiffer : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem == newItem
        }
    }
}