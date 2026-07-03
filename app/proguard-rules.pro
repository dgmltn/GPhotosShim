# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn io.ktor.utils.io.jvm.nio.WritingKt

# Ensure all Previews have been stripped
# https://issuetracker.google.com/issues/157891235#comment6
-checkdiscard class * { @androidx.compose.ui.tooling.preview.Preview <methods>; }
-keepclassmembers,allowshrinking class * { @androidx.compose.ui.tooling.preview.Preview <methods>; }
