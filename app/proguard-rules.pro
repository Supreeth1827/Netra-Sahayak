# Retrofit / Gson response models are created reflectively.
-keep class com.sih.netrasahayak.network.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
