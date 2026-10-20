# Room Database Proguard Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Moshi rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn sun.misc.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Domain & Data Models
-keep class com.example.data.model.** { *; }
-keep class com.example.domain.** { *; }

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
