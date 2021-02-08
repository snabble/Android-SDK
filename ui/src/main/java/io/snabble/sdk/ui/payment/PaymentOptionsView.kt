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
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.loadAsset
import java.util.*
import kotlin.collections.ArrayList

class PaymentOptionsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.snabble_payment_options, this)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val adapter = EntryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter.submitList(getEntries())
    }

    private fun getEntries(): List<Entry> {
        val paymentMethods = paymentMethods()
        val projectsWithCreditCards = projectsWithCreditCards()
        val globalList = ArrayList<Entry>()
        val projectList = ArrayList<Entry>()

        globalList.add(
            Entry(
                isSectionHeader = true,
                text = "Händlerübergreifend",
            )
        )

        if (paymentMethods.contains(PaymentMethod.DE_DIRECT_DEBIT)) {
            globalList.add(
                Entry(
                    text = "SEPA",
                    icon = R.drawable.snabble_ic_payment_select_sepa,
                    click = {
                        executeUiAction(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT)
                    }
                )
            )
        }

        if (paymentMethods.contains(PaymentMethod.PAYDIREKT)) {
            globalList.add(
                Entry(
                    text = "Paydirekt",
                    icon = R.drawable.snabble_ic_payment_select_paydirekt,
                    click = {
                        executeUiAction(SnabbleUI.Action.SHOW_PAYDIREKT_INPUT)
                    }
                )
            )
        }

        projectList.add(
            Entry(
                isSectionHeader = true,
                text = "Für einzelnen Händler",
            )
        )

        projectsWithCreditCards.forEach { project ->
            val projectsWithSameBrand = Snabble.getInstance().projects.filter {
                it.brand?.name == project.brand?.name
            }

            projectList.add(
                Entry(
                    text = project.name,
                    project = project,
                    click = {
                        if (projectsWithSameBrand.isEmpty()) {
                            val activity = UIUtils.getHostActivity(context)
                            if (activity is FragmentActivity) {
                                val dialogFragment = SelectPaymentMethodFragment()
                                val args = Bundle()
                                args.putSerializable(SelectPaymentMethodFragment.ARG_PAYMENT_METHOD_LIST, java.util.ArrayList(listOf(
                                    PaymentMethod.VISA,
                                    PaymentMethod.MASTERCARD,
                                    PaymentMethod.AMEX))
                                )
                                dialogFragment.arguments = args
                                dialogFragment.show(activity.supportFragmentManager, null)
                            } else {
                                throw RuntimeException("Host activity needs to be a FragmentActivity")
                            }
                        } else {
                            executeUiAction(SnabbleUI.Action.SHOW_PROJECT_PAYMENT_OPTIONS, projectsWithSameBrand)
                        }
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
        val icon: Int? = null,
        val project: Project? = null,
        val click: OnClickListener? = null,
    )

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
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