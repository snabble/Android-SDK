package io.snabble.sdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.snabble.sdk.Snabble

class NotCheckedInFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_not_checked_in, container, false).apply {
            (this.context as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val button = view.findViewById<Button>(R.id.check_in_now)
        button.setOnClickListener {
            Snabble.checkedInShop = Snabble.projects.first().shops.first()
            findNavController().popBackStack()
        }
    }
}