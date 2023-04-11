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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Brand
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentialsStore
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.getFragmentActivity
import io.snabble.sdk.ui.utils.loadAsset
import kotlin.collections.set

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
        adapter.submitList(getEntries())

        val listener = PaymentCredentialsStore.Callback {
            adapter.submitList(getEntries())
        }

        Snabble.paymentCredentialsStore.addCallback(listener)

        getFragmentActivity()?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adapter.submitList(getEntries())
                    Lifecycle.Event.ON_DESTROY -> {
                        getFragmentActivity()?.lifecycle?.removeObserver(this)
                        Snabble.paymentCredentialsStore.removeCallback(listener)
                    }
                    else -> Unit
                }
            }
        })
    }

    private fun getEntries(): List<Entry> {
        val projects = Snabble.projects.filter { project ->
            project.availablePaymentMethods.count { it.isRequiringCredentials } > 0
        }

        val projectList = ArrayList<Entry>()
        val store = Snabble.paymentCredentialsStore

        projects
            .filter { it.brand == null }
            .forEach { project ->
                val count = store.getCountForProject(project)

                projectList.add(
                    Entry(
                        text = project.name,
                        project = project,
                        count = count,
                        click = {
                            if (count > 0) {
                                PaymentInputViewHelper.showPaymentList(context, project)
                            } else {
                                PaymentInputViewHelper.showPaymentSelectionForAdding(context, project)
                            }
                        }
                    )
                )
            }

        val brands = ArrayList<Brand>()
        val counts = HashMap<Brand, Int>()
        val projectCount = HashMap<Brand, Int>()

        projects.forEach { project ->
            project.brand?.let { brand ->
                val count = store.getCountForProject(project)

                if (!brands.contains(project.brand)) {
                    brands.add(brand)
                }

                val currentCount = counts.getOrPut(brand) { 0 }
                counts[brand] = currentCount + count
                projectCount[brand] = projectCount.getOrPut(brand) { 0 } + 1
            }
        }

        brands.forEach { brand ->
            val project = projects.firstOrNull { it.brand?.id == brand.id }
            val projectBrandCount = projectCount[project?.brand] ?: 0
            projectList.add(
                Entry(
                    text = brand.name,
                    project = project,
                    count = counts[brand] ?: 0,
                    click = {
                        if (project != null && projectBrandCount <= 1) {
                            PaymentInputViewHelper.showPaymentList(context, project)
                        } else {
                            val args = Bundle()
                            args.putString(ProjectPaymentOptionsView.ARG_BRAND, brand.id)
                            executeUiAction(SnabbleUI.Event.SHOW_PROJECT_PAYMENT_OPTIONS, args)
                        }

                    }
                )
            )
        }

        val adapterList = ArrayList<Entry>()
        adapterList.addAll(projectList)

        return adapterList
    }

    data class Entry(
        val text: String? = null,
        val count: Int = 0,
        val icon: Int? = null,
        val project: Project? = null,
        val click: OnClickListener? = null,
    )

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val text: TextView = itemView.findViewById(R.id.text)
        val count: TextView = itemView.findViewById(R.id.count)
        val image: ImageView = itemView.findViewById(R.id.helper_image)
    }

    class EntryAdapter : ListAdapter<Entry, EntryViewHolder>(EntryItemDiffer()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return EntryViewHolder(inflater.inflate(R.layout.snabble_item_payment_options_entry, parent, false))
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val entry = getItem(position)

            holder.text.text = entry.text

            if (entry.icon != null) {
                holder.image.setImageResource(entry.icon)
            } else {
                holder.image.setImageBitmap(null)
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

    class EntryItemDiffer : DiffUtil.ItemCallback<Entry>() {

        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem == newItem
        }
    }
}
