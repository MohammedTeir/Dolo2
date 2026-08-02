# Proguard rules for core-engine consumers
# youtubedl-android uses Jackson for JSON deserialization
-keep class com.yausername.youtubedl_android.mapper.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Room entities
-keep class com.dolo.core.db.** { *; }
