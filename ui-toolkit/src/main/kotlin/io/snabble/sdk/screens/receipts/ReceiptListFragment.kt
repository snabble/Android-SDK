package io.snabble.sdk.screens.receipts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.snabble.sdk.ui.toolkit.R

class ReceiptListFragment : Fragment() {

    private lateinit var receiptListAdapter: ReceiptListAdapter

    private lateinit var emptyView: View
    private lateinit var progressView: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.fragment_receipt_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        receiptListAdapter = ReceiptListAdapter(viewLifecycleOwner.lifecycleScope)

        emptyView = view.findViewById(R.id.empty_state)
        progressView = view.findViewById(R.id.progress)

        view.findViewById<RecyclerView>(R.id.recycler_view)
            .apply {
                adapter = receiptListAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                addItemDecoration(DividerItemDecoration(context, VERTICAL))
            }

        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
            .apply {
                setOnRefreshListener(ReceiptManager::update)
            }

        ReceiptManager.receiptInfo.observe(viewLifecycleOwner) { receiptInfoList ->
            emptyView.isVisible = receiptInfoList.isNullOrEmpty()
            receiptListAdapter.receiptInfoList = receiptInfoList
        }

        ReceiptManager.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressView.isVisible = isLoading
            if (!isLoading) swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()

        ReceiptManager.update()
    }

    private fun setupToolbar() {
        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar ?: return

        supportActionBar.setDisplayHomeAsUpEnabled(true)
    }
}
