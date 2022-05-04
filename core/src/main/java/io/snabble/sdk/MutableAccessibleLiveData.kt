package io.snabble.sdk

/**
 * Mutable LiveData with the current (possible not yet published) value. You
 * can observe changes like with regular MutableLiveData, just not inherited.
 * You can write from any thread the value it will be posted on the main thread
 * and published there when called from a background thread.
 *
 * Please be aware that an RuntimeException will be thrown when you access
 * value without setting a value ether with the setter or the constructor.
 *
 * You can directly compare the value with the LiveData's value directly as
 * syntactic sugar.
 */
class MutableAccessibleLiveData<T>: AccessibleLiveData<T> {
    constructor() : super()
    constructor(initialValue: T) : super(initialValue)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @set:JvmName("setLatestValue")
    @get:JvmName("getLatestValue")
    override var value: T
        get() = super.value
        public set(value) { super.value = value }

    public override fun setValue(value: T) {
        this.value = value
    }
}