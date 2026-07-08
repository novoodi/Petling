# Petling ProGuard/R8 규칙 (release minify+shrink 활성화 대응)

# 스택트레이스 가독성(Crashlytics 심볼화에 필요)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# kotlinx.serialization
# @Serializable 클래스의 생성된 Companion serializer()가 R8에 제거/난독화되지 않게 유지
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
# 앱의 직렬화 대상(파싱 draft 등) 유지
-keep,includedescriptorclasses class com.example.petling.**$$serializer { *; }
-keepclassmembers class com.example.petling.** {
    *** Companion;
}

# ---------------------------------------------------------------------------
# Room
# 엔티티/DAO는 R8과 대체로 호환되나, 리플렉션 경로 방어를 위해 유지
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# ML Kit GenAI (Gemini Nano, beta) + ML Kit Text Recognition
# 온디바이스 AI 경로가 난독화로 깨지지 않게 유지
# ---------------------------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ---------------------------------------------------------------------------
# Kotlin 코루틴 / 일반
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
