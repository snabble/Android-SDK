package io.snabble.sdk.utils.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import java.io.File

class FileExclusionStrategy : ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes?): Boolean =
        f?.declaringClass == File::class.java

    override fun shouldSkipClass(clazz: Class<*>?): Boolean =
        clazz == File::class.java
}
