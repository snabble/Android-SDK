@file:JvmName("FileUtilsSupport")

package io.snabble.sdk.utils

import android.os.Build
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object FileUtilsSupport {

    @JvmStatic
    @Throws(IOException::class)
    fun moveFile(srcFile: File, dstFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FileUtils.moveFile(srcFile, dstFile)
        } else {
            moveFileSupport(srcFile, dstFile)
        }
    }

    private fun moveFileSupport(srcFile: File, dstFile: File) {
        try {
            val isRenamed = srcFile.renameTo(dstFile)
            if (!isRenamed) {
                copyFile(srcFile, dstFile)
            }
        } catch (_: SecurityException) {
            Logger.d("No write access to either srcFile, dstFile or both.")
        } catch (_: NullPointerException) {
            Logger.d("dstFile must not be null.")
        }
        FileUtils.deleteQuietly(srcFile)
    }

    private fun copyFile(srcFile: File, dstFile: File) {
        try {
            FileOutputStream(dstFile).channel.use { dstChannel ->
                FileInputStream(srcFile).channel.use { srcChannel ->
                    srcChannel.transferTo(
                        0,
                        srcChannel.size(),
                        dstChannel
                    )
                }
            }
        } catch (_: FileNotFoundException) {
            Logger.d("Either srcFile, dstFile or both do not exist.")
        } catch (_: SecurityException) {
            Logger.d("No write access to either srcFile, destFile or both.")
        } catch (exception: IOException) {
            Logger.e("Could not write srcFile to dstFile: %s", exception.toString())
        }
    }
}
