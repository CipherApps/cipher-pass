-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.* *;
}

-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keepclasseswithmembernames class * {
    @androidx.room.* *;
}

-keepclassmembers class * {
    *** component1();
    *** component2();
    *** component3();
    *** component4();
    *** component5();
}

-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

-keep class androidx.navigation.** { *; }
-keepclasseswithmembernames class * {
    @androidx.navigation.* *;
}

-keep class androidx.compose.** { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keep class dev.cipher.pass.BuildConfig { *; }
