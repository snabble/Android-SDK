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
