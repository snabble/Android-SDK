package io.snabble.sdk.shoppingcart

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonSyntaxException
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import kotlin.time.Duration.Companion.seconds

internal class ShoppingCartStorage(val project: Project) {

    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val fileMap = mutableMapOf<String, File>()
    private var currentFile: File? = null

    init {
        updateFileMap()
        load()

        Dispatch.mainThread {
            project.shoppingCart.addListener(object : SimpleShoppingCartListener() {
                override fun onChanged(cart: ShoppingCart) {
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
                        return@observeForever
                    }
                }

                currentFile = null
            }
        }
    }

    private fun updateFileMap() {
        val env = Snabble.environment?.name?.lowercase() ?: "unknown"
        Dispatch.mainThread {
            project.shops.forEach {
                fileMap[it.id] =
                    File(project.internalStorageDirectory, "cart/$env/${it.id}/shoppingCart.json")
            }
        }
    }

    private fun load() {
        try {
            if (currentFile?.exists() == true) {
                val contents: String? =
                    IOUtils.toString(FileInputStream(currentFile), Charset.forName("UTF-8"))
                val shoppingCartData: ShoppingCartData? =
                    GsonHolder.get().fromJson(contents, ShoppingCartData::class.java)
                if (shoppingCartData != null) {
                    project.shoppingCart.initWithData(shoppingCartData)
                } else {
                    //shopping cart could not be read, create a new one.
                    project.shoppingCart.initWithData(ShoppingCartData())
                }
            } else {
                project.shoppingCart.initWithData(ShoppingCartData())
            }
        } catch (e: IOException) {
            //shopping cart could not be read, create a new one.
            Logger.e("Could not load shopping list from: " + currentFile?.absolutePath + ", creating a new one.")
            project.shoppingCart.initWithData(ShoppingCartData())
        } catch (e: JsonSyntaxException) {
            //shopping cart could not be read, create a new one.
            Logger.e("Could not parse shopping list due to: ${e.message}")
            project.shoppingCart.initWithData(ShoppingCartData())
        }
    }

    private fun saveDebounced() {
        val file = currentFile
        val data = project.shoppingCart.data

        if (file != null) {
            mainThreadHandler.removeCallbacksAndMessages(null)
            mainThreadHandler.postDelayed({
                val json = GsonHolder.get().toJson(data)
                Dispatch.background {
                    try {
                        FileUtils.forceMkdirParent(file)
                        IOUtils.write(json, FileOutputStream(file), Charset.forName("UTF-8"))
                    } catch (e: IOException) {
                        //could not save shopping cart, silently ignore
                        Logger.e("Could not save shopping list: " + e.message)
                    }
                }
            }, 1.seconds.inWholeMilliseconds)
        }
    }
}
