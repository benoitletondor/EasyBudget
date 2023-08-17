# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/benoit/Android/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class * extends androidx.fragment.app.Fragment {}

# This is generated automatically by the Android Gradle plugin.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn com.fasterxml.jackson.annotation.JsonFormat
-dontwarn com.fasterxml.jackson.databind.DeserializationContext
-dontwarn com.fasterxml.jackson.databind.JsonDeserializer
-dontwarn com.fasterxml.jackson.databind.JsonSerializer
-dontwarn com.fasterxml.jackson.databind.SerializerProvider
-dontwarn com.fasterxml.jackson.databind.module.SimpleModule
-dontwarn com.fasterxml.jackson.databind.ser.std.StdSerializer

# https://github.com/mangstadt/biweekly/issues/98
-keep class biweekly.** { *; }