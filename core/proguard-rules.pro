#-dontwarn java.lang.invoke.**
#
## OkHttp
#-keepattributes Signature
#-keepattributes *Annotation*
#-keep class okhttp3.** { *; }
#-keep interface okhttp3.** { *; }
#-dontwarn okhttp3.**
#
## Okio
#-keep class sun.misc.Unsafe { *; }
#-dontwarn java.nio.file.*
#-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
#-dontwarn okio.**
#
## Picasso
#-dontwarn com.squareup.okhttp.**
#
## Gson
#-keepattributes Signature
#-keepattributes *Annotation*
#-dontwarn sun.misc.**
#-keep class * implements com.google.gson.TypeAdapterFactory
#-keep class * implements com.google.gson.JsonSerializer
#-keep class * implements com.google.gson.JsonDeserializer

# Gson: keep any field annotated with @SerializedName so R8 cannot rename or remove it.
# This is the single rule that makes all @SerializedName annotations work under R8 strict mode.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson needs generic type signatures to deserialize parameterized types (e.g. List<Foo>).
-keepattributes Signature
-keepattributes *Annotation*

# Retain TypeToken so Gson can resolve generic types at runtime.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# SDK
# Keep enum values()/valueOf() for Gson @SerializedName resolution.
# Keep public static final fields so .name() returns the declared constant name,
# not the obfuscated one — required for resource lookup in SnabbleInitializer.
-keepclassmembers enum io.snabble.sdk.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static final ** *;
}

# Keep Parcelable CREATOR — required for cross-process parcel reconstruction
-keep class io.snabble.sdk.** implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable lifecycle methods
-keepclassmembers class io.snabble.sdk.** implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
