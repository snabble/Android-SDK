package io.snabble.sdk

import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

inline infix fun <reified T : Any> T.merge(other: T): T {
    val nameToProperty = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associateWith { parameter ->
        val property = nameToProperty[parameter.name]!!
        (property.get(other) ?: property.get(this))
    }
    return primaryConstructor.callBy(args)
}

@Throws(InterruptedException::class)
fun <T> LiveData<T>.getOrAwaitValue(): T? {
    val data = arrayOfNulls<Any>(1)
    val latch = CountDownLatch(1)
    val observer: Observer<T> = object : Observer<T> {
        override fun onChanged(@Nullable o: T) {
            data[0] = o
            latch.countDown()
            removeObserver(this)
        }
    }
    observeForever(observer)
    if (!latch.await(2, TimeUnit.SECONDS)) {
        throw RuntimeException("LiveData value was never set.")
    }
    return data[0] as T?
}