# kotlinx-serialization：保留备份 DTO 及其 Companion（序列化器）
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations
-keep class com.personal.ledger.data.backup.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerializerFor *;
}
