package io.snabble.sdk.ui

import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

abstract class BaseFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typedValue = TypedValue()
        if (theme.resolveAttribute(R.attr.snabbleToolbarStyle, typedValue, true)) {
            setContentView(R.layout.snabble_activity_simple_fragment_with_toolbar)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
        } else {
            setContentView(R.layout.snabble_activity_simple_fragment)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, onCreateFragment())
            .commitAllowingStateLoss()
    }

    abstract fun onCreateFragment(): Fragment
}
