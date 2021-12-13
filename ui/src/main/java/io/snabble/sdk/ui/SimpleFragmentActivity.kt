package io.snabble.sdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class SimpleFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.snabble_activity_simple_fragment)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, onCreateFragment())
            .commitAllowingStateLoss()
    }

    abstract fun onCreateFragment(): Fragment
}