# kotlinx-serialization：保留备份 DTO 及其 Companion（序列化器）
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations
-keep class io.github.twc528491.betterledger.data.backup.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerializerFor *;
}
