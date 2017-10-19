-keep class com.simplemobiletools.** { *; }
-dontwarn com.simplemobiletools.**

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-dontwarn com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
-dontwarn com.bumptech.glide.load.resource.bitmap.Downsampler
-dontwarn com.bumptech.glide.load.resource.bitmap.HardwareConfigState
-dontwarn com.bumptech.glide.manager.RequestManagerRetriever
