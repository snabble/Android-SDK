package io.snabble.sdk

import androidx.lifecycle.LiveData
import io.snabble.sdk.utils.Dispatch

/**
 * LiveData with the current (possible not yet published) value. You can
 * observe changes like with regular LiveData. You can write from any thread
 * the value it will be posted on the main thread and published there when
 * called from a background thread.
 *
 * Please be aware that an RuntimeException will be thrown when you access
 * value without setting a value ether with the setter or the constructor.
 *
 * You can directly compare the value with the LiveData's value directly as
 * syntactic sugar.
 */
open class AccessibleLiveData<T>: LiveData<T> {
    private val NOT_SET = Any()
    private var latestValue : Any? = NOT_SET

    constructor(): super()
    constructor(initialValue: T): super(initialValue) {
        latestValue = initialValue
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @set:JvmName("setLatestValue")
    @get:JvmName("getLatestValue")
    open var value: T
        protected set(value) {
            if (latestValue != value) {
                latestValue = value
                Dispatch.mainThread {
                    super.setValue(value)
                }
            }
        }
        get() = if (latestValue !== NOT_SET) {
            @Suppress("UNCHECKED_CAST")
            latestValue as T
        } else throw RuntimeException("No value set. Please provide a default " +
                "value, set a value before usage or use the hasValue() method.")

    fun hasValue() = latestValue !== NOT_SET

    override fun setValue(value: T) {
        this.value = value
    }

    override fun getValue(): T = this.value

    override fun postValue(value: T) {
        this.value = value
    }

    override fun equals(other: Any?) = other === this || other === value

    override fun hashCode() = latestValue?.hashCode() ?: 0
}