-dontobfuscate

# This is broad, but better leave a few common class and still optimize the rest out
-keep class net.taler.common.** {*;}

# AndroidX navigation
-keepnames class androidx.navigation.fragment.NavHostFragment

# Jackson serialization
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# KotlinX serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
