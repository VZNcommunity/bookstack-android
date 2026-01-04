# BookStack Android App - ProGuard Rules (2026-01-05)
# Add project specific ProGuard rules here.

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Keep Room entities
-keep class com.vzith.bookstack.data.db.entity.** { *; }

# Keep Gson serialization
-keep class com.vzith.bookstack.data.api.** { *; }
