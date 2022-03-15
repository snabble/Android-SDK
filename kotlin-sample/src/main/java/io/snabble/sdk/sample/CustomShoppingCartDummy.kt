package io.snabble.sdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.SnabbleUI

class CustomShoppingCartDummy : Fragment() {
    private var cart: ShoppingCart? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_cart_dummy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val adapter = DummyAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        SnabbleUI.projectAsLiveData.observe(viewLifecycleOwner) {
            cart = it?.shoppingCart
            adapter.notifyDataSetChanged()
        }
    }

    class DummyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var text: TextView = itemView.findViewById(R.id.text)
    }

    inner class DummyAdapter : RecyclerView.Adapter<DummyVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DummyVH {
            val inflater = LayoutInflater.from(requireContext())
            return DummyVH(inflater.inflate(R.layout.item_dummy_cart_entry, parent, false))
        }

        override fun onBindViewHolder(holder: DummyVH, position: Int) {
            holder.text.text = cart?.get(position)?.displayName
        }

        override fun getItemCount(): Int {
            return cart?.size() ?: 0
        }
    }
}