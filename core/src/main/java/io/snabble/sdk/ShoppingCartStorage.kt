package io.snabble.sdk

import android.os.Handler
import io.snabble.sdk.utils.GsonHolder
import android.os.Looper
import io.snabble.sdk.ShoppingCart.SimpleShoppingCartListener
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.charset.Charset

internal class ShoppingCartStorage(project: Project,
                                   shopId: String) {
    private val project: Project
    private val file: File
    private val mainThreadHandler: Handler

    lateinit var shoppingCart: ShoppingCart
        private set

    init {
        mainThreadHandler = Handler(Looper.getMainLooper())
        this.project = project
        file = File(project.internalStorageDirectory, shopId + "/shoppingCart.json")
        load()
        shoppingCart.addListener(object : SimpleShoppingCartListener() {
            override fun onChanged(list: ShoppingCart) {
                saveDebounced()
            }
        })
    }

    private fun load() {
        try {
            if (file.exists()) {
                val contents = IOUtils.toString(FileInputStream(file), Charset.forName("UTF-8"))
                shoppingCart = GsonHolder.get().fromJson(contents, ShoppingCart::class.java)
                shoppingCart.initWithProject(project)
            } else {
                shoppingCart = ShoppingCart(project)
            }
        } catch (e: Exception) {
            //shopping cart could not be read, create a new one.
            Logger.e("Could not load shopping list from: " + file.absolutePath + ", creating a new one.")
            shoppingCart = ShoppingCart(project)
        }
    }

    private fun saveDebounced() {
        mainThreadHandler.removeCallbacksAndMessages(null)
        mainThreadHandler.postDelayed({
            val json = shoppingCart.toJson()
            Dispatch.background {
                try {
                    FileUtils.forceMkdirParent(file)
                    IOUtils.write(json, FileOutputStream(file), Charset.forName("UTF-8"))
                } catch (e: IOException) {
                    //could not save shopping cart, silently ignore
                    Logger.e("Could not save shopping list for " + project.id + ": " + e.message)
                }
            }
        }, 1000)
    }
}