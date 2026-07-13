# Retrofit / Gson models
-keep class com.example.storemobile.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
