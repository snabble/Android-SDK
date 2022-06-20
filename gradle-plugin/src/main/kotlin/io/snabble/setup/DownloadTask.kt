package io.snabble.setup

import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * The task to download the snabble metadata.
 */
@CacheableTask
abstract class DownloadTask : DefaultTask() {
    //init {
    //    outputs.upToDateWhen { task ->
    //        // TODO 24h caching
    //        outputFile.exists()
    //    }
    //}

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract var outputFile: File

    /**
     * Start the download of the metadata.
     */
    @TaskAction
    fun download() {
        val outputDirFile = outputFile.parentFile
        if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
            throw IOException("Could not create path $outputDirFile")
        }

        val response = OkHttpClientFactory.createOkHttpClient()
            .newCall(
                Request.Builder()
                .url(url.get())
                .build()
            ).execute()
        if (response.isSuccessful) {
            val progressLogger = ProgressLoggerWrapper(logger, services, "Manifest")
            response.body?.byteStream()?.safeToFileWithTempFile(outputFile, progressLogger)
        } else {
            logger.error("Failed to download '${url.get()}'. Server returned status ${response.code}")
        }
    }

    /**
     * Copy bytes from an input stream to a file and log progress.
     *
     * @param is the input stream to read
     * @param destFile the file to write to
     * @param progressLogger progress logger
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun InputStream.safeToFile(destFile: File, progressLogger: ProgressLoggerWrapper) {
        try {
            progressLogger.started()
            var finished = false
            try {
                AsynchronousFileChannel.open(
                    destFile.toPath(), StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
                ).use { channel ->
                    var pos: Long = 0
                    var writeFuture: Future<Int?>? = null
                    var buf1 = ByteArray(1024 * 10)
                    var buf2 = ByteArray(1024 * 10)
                    var bb1: ByteBuffer = ByteBuffer.wrap(buf1)
                    var bb2: ByteBuffer = ByteBuffer.wrap(buf2)
                    var read: Int
                    while ((this.read(buf1).also { read = it }) >= 0) {
                        writeFuture?.get()
                        bb1.position(0)
                        bb1.limit(read)
                        writeFuture = channel.write(bb1, pos)
                        pos += read.toLong()
                        progressLogger.incrementProgress(read.toLong())

                        // swap buffers for next asynchronous operation
                        val tmpBuf: ByteArray = buf1
                        buf1 = buf2
                        buf2 = tmpBuf
                        val tmpBB: ByteBuffer = bb1
                        bb1 = bb2
                        bb2 = tmpBB
                    }
                    writeFuture?.get()
                    finished = true
                }
            } catch (e: InterruptedException) {
                throw IOException("Writing to destination file was interrupted", e)
            } catch (e: ExecutionException) {
                if (e.cause is IOException) {
                    throw e.cause as IOException
                }
                throw IOException("Could not write to destination file", e)
            } finally {
                if (!finished) {
                    destFile.delete()
                }
            }
        } finally {
            this.close()
            progressLogger.completed()
        }
    }

    /**
     * Copy bytes from an input stream to a temporary file and log progress. Upon successful
     * completion, move the temporary file to the given destination.
     *
     * @param destFile the destination file
     * @param progressLogger progress logger
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun InputStream.safeToFileWithTempFile(destFile: File, progressLogger: ProgressLoggerWrapper) {
        //create name of temporary file
        val tempFile = File.createTempFile(destFile.name, ".part", destFile.parentFile)

        //stream and move
        this.safeToFile(tempFile, progressLogger)
        if (destFile.exists()) {
            //Delete destFile if it exists before renaming tempFile.
            //Otherwise renaming might fail.
            if (!destFile.delete()) {
                throw IOException("Could not delete old destination file '${destFile.absolutePath}'.")
            }
        }
        try {
            moveFile(tempFile, destFile, progressLogger)
        } catch (e: IOException) {
            throw IOException(
                "Failed to move temporary file '${tempFile.absolutePath}' to destination " +
                        "file '${destFile.absolutePath}'.", e)
        }
    }

    /**
     * Move a file by calling [File.renameTo] first and, if this fails, by copying and deleting it.
     * @param src the file to move
     * @param dest the destination
     * @param progressLogger progress logger
     * @throws IOException if the file could not be moved
     */
    @Throws(IOException::class)
    private fun moveFile(src: File, dest: File, progressLogger: ProgressLoggerWrapper) {
        if (src.renameTo(dest)) {
            return
        }
        FileInputStream(src).use { stream -> stream.safeToFile(dest, progressLogger) }
        if (!src.delete()) {
            throw IOException(
                "Could not delete temporary file '${src.absolutePath}' after copying it to '${dest.absolutePath}'."
            )
        }
    }
}