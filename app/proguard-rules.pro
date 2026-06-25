# StreamFlow Player ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.streamflow.player.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
