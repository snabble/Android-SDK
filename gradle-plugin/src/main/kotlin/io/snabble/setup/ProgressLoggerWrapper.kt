package io.snabble.setup

import org.gradle.api.logging.Logger
import org.gradle.internal.service.ServiceRegistry
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

typealias ProgressLogger = Any

/**
 * Wraps around Gradle's internal progress logger. Uses reflection
 * to provide as much compatibility to different Gradle versions
 * as possible. Note that Gradle's progress logger does not belong
 * to its public API.
 *
 * Based on Michel Kraemer's gradle-download-task: https://github.com/michel-kraemer/gradle-download-task/blob/521680761669ec8a004236b756b74ea28e6ed575/src/main/java/de/undercouch/gradle/tasks/download/internal/ProgressLoggerWrapper.java
 *
 * @param logger the Logger
 * @param services the ServiceRegistry
 * @param src the URL to the file to be downloaded
 */
class ProgressLoggerWrapper(
    private val logger: Logger,
    services: ServiceRegistry,
    src: String) {
    private var progressLogger: ProgressLogger?
    private var size: String? = null
    private var destFileName: String? = null
    private var processedBytes: Long = 0
    private var loggedKb: Long = 0

    init {
        // we are about to access an internal class. Use reflection here to provide
        // as much compatibility to different Gradle versions as possible

        // get ProgressLoggerFactory class
        val progressLoggerFactoryClass = Class.forName("org.gradle.internal.logging.progress.ProgressLoggerFactory")

        //get ProgressLoggerFactory service
        val progressLoggerFactory = services("get", progressLoggerFactoryClass)

        //get actual progress logger
        progressLogger = progressLoggerFactory("newOperation", javaClass)

        //configure progress logger
        val description = "Download $src"
        progressLogger?.invoke( "setDescription", description)
        try {
            // prior to Gradle 6.0
            progressLogger?.invoke("setLoggingHeader", description)
        } catch (e: ReflectiveOperationException) {
            logger.lifecycle(description)
        }
    }

    /**
     * Invoke a method using reflection but don't throw any exceptions.
     * Just log errors instead.
     * @param obj the object whose method should be invoked
     * @param method the name of the method to invoke
     * @param args the arguments to pass to the method
     */
    private fun Any?.invokeIgnoreExceptions(method: String, vararg args: Any) = this?.let {
        try {
            invoke(method, *args)
        } catch (e: NullPointerException) { // FIXME just used for completed()
            logger.trace("Unable to log progress", e)
        } catch (e: ReflectiveOperationException) {
            logger.trace("Unable to log progress", e)
        }
    }

    /**
     * Start on operation
     */
    fun started() {
        progressLogger.invokeIgnoreExceptions("started")
    }

    /**
     * Complete an operation
     */
    fun completed() {
        progressLogger.invokeIgnoreExceptions("completed")
    }

    /**
     * Set the current operation's progress
     * @param msg the progress message
     */
    private fun progress(msg: String) {
        progressLogger.invokeIgnoreExceptions("progress", msg)
    }

    /**
     * The total number of bytes to process and reset progress
     * @param size the total size
     */
    fun setSize(size: Long) {
        this.size = size.toHumanReadableLength()
        processedBytes = 0
        loggedKb = 0
    }

    /**
     * Set the name of the destination file
     * @param destFileName the file name
     */
    fun setDestFileName(destFileName: String?) {
        this.destFileName = destFileName
    }

    /**
     * Increment the number of bytes processed
     * @param increment the increment
     */
    fun incrementProgress(increment: Long) {
        processedBytes += increment
        val processedKb = processedBytes / 1024
        if (processedKb > loggedKb) {
            val sb = StringBuilder()
            if (destFileName != null) {
                sb.append(destFileName)
                sb.append(" > ")
            }
            sb.append(processedBytes.toHumanReadableLength())
            if (size != null) {
                sb.append("/")
                sb.append(size)
            }
            sb.append(" downloaded")
            progress(sb.toString())
            loggedKb = processedKb
        }
    }

    companion object {
        /**
         * Invoke a method using reflection
         * @param obj the object whose method should be invoked
         * @param method the name of the method to invoke
         * @param args the arguments to pass to the method
         * @return the method's return value
         * @throws NoSuchMethodException if the method was not found
         * @throws InvocationTargetException if the method could not be invoked
         * @throws IllegalAccessException if the method could not be accessed
         */
        @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
        private operator fun ProgressLogger.invoke(method: String, vararg args: Any): Any {
            val argumentTypes = args.map { it::class.java }.toTypedArray()
            val m = findMethod(this, method, argumentTypes)
            m.isAccessible = true
            return m.invoke(this, *args)
        }

        /**
         * Uses reflection to find a method with the given name and argument types
         * from the given object or its superclasses.
         * @param obj the object
         * @param methodName the name of the method to return
         * @param argumentTypes the method's argument types
         * @return the method object
         * @throws NoSuchMethodException if the method could not be found
         */
        @Throws(NoSuchMethodException::class)
        private fun findMethod(
            obj: Any, methodName: String,
            argumentTypes: Array<Class<*>>
        ): Method {
            var clazz: Class<*>? = obj.javaClass
            while (clazz != null) {
                val methods = clazz.declaredMethods
                for (method: Method in methods) {
                    if (method.name == methodName &&
                        Arrays.equals(method.parameterTypes, argumentTypes)
                    ) {
                        return method
                    }
                }
                clazz = clazz.superclass
            }
            throw NoSuchMethodException("Method $methodName(${argumentTypes.joinToString()}) on " + obj.javaClass)
        }

        private const val kb = 1024
        private const val mb = kb * 1024
        private const val gb = mb * 1024
        private val bytes = 0 .. kb
        private val kilobytes = kb + 1 .. mb
        private val megabytes = mb + 1 .. Long.MAX_VALUE
        private val invalid = Long.MIN_VALUE..-1

        /**
         * Converts a number of bytes to a human-readable string
         * @return the human-readable string
         */
        private fun Long.toHumanReadableLength() = when(this) {
            in invalid -> "Unknown"
            1L -> "1 Byte"
            in bytes -> "$this Bytes"
            in kilobytes -> (this / kb).toString() + " KB"
            in megabytes -> String.format("%.2f MB", this / mb.toDouble())
            else -> String.format("%.2f GB", this / gb.toDouble())
        }
    }
}