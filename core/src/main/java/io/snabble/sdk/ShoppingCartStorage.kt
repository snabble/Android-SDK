package io.snabble.sdk

import android.os.Handler
import io.snabble.sdk.utils.GsonHolder
import android.os.Looper
import android.util.Log
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

internal class ShoppingCartStorage(project: Project) {
    private val project: Project
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val fileMap = mutableMapOf<String, File>()
    private var currentFile: File? = null

    init {
        this.project = project
        updateFileMap()
        load()

        Dispatch.mainThread {
            project.shoppingCart.addListener(object : SimpleShoppingCartListener() {
                override fun onChanged(list: ShoppingCart) {
                    saveDebounced()
                }
            })

            project.addOnUpdateListener {
                updateFileMap()
            }

            Snabble.currentCheckedInShop.observeForever { shop ->
                if (shop != null) {
                    if (project.shops.any { it.id == shop.id }) {
                        currentFile = fileMap[shop.id]
                        load()
                    }
                }
            }
        }

    }

    private fun updateFileMap() {
        Dispatch.mainThread {
            project.shops.forEach {
                fileMap[it.id] = File(project.internalStorageDirectory, "${it.id}/shoppingCart.json")
            }
        }
    }

    private fun load() {
        try {
            if (currentFile?.exists() == true) {
                val contents = IOUtils.toString(FileInputStream(currentFile), Charset.forName("UTF-8"))
                val shoppingCartData = GsonHolder.get().fromJson(contents, ShoppingCartData::class.java)
                project.shoppingCart.initWithData(shoppingCartData)
                Log.d("Cart", "Init cart " + currentFile?.absolutePath)
            } else {
                Log.d("Cart", "Init new cart")
                project.shoppingCart.initWithData(ShoppingCartData())
            }
        } catch (e: Exception) {
            Log.d("Cart", "Init new cart error: " + e.message)
            //shopping cart could not be read, create a new one.
            Logger.e("Could not load shopping list from: " + currentFile?.absolutePath + ", creating a new one.")
            project.shoppingCart.initWithData(project, ShoppingCartData())
        }
    }

    private fun saveDebounced() {
        mainThreadHandler.removeCallbacksAndMessages(null)
        mainThreadHandler.postDelayed({
            val json = GsonHolder.get().toJson(project.shoppingCart.data)
            Dispatch.background {
                try {
                    FileUtils.forceMkdirParent(currentFile)
                    IOUtils.write(json, FileOutputStream(currentFile), Charset.forName("UTF-8"))
                } catch (e: IOException) {
                    //could not save shopping cart, silently ignore
                    Logger.e("Could not save shopping list for " + project.id + ": " + e.message)
                }
            }
        }, 1000)
    }
}