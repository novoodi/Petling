# Firebase Crashlytics 도입 가이드 (H1)

크래시 리포팅으로 출시 후 안정성(크래시율·ANR, 특히 Gemini Nano 경로 예외)을 모니터링한다.
`google-services.json`이 없으면 google-services 플러그인이 빌드를 깨므로, **아래 순서대로** 활성화한다.
(현재 저장소는 빌드가 깨지지 않도록 플러그인을 아직 적용하지 않은 상태다.)

## 사전 준비 (사용자, 외부 작업)
1. https://console.firebase.google.com 에서 프로젝트 생성.
2. Android 앱 추가 — **패키지명은 반드시 `com.novoodi.petling`** (applicationId와 일치).
3. `google-services.json` 다운로드 → `app/google-services.json`에 저장. (이미 `.gitignore`로 커밋 제외됨)
4. Firebase 콘솔에서 Crashlytics 활성화.

## 코드/Gradle 활성화 (google-services.json 추가 후)

### 1) `gradle/libs.versions.toml` — 이미 아래 항목을 추가해 둠(주석 참고), 없으면 추가
```toml
[versions]
googleServices = "4.4.2"
firebaseBom = "33.7.0"
firebaseCrashlyticsPlugin = "3.0.2"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsPlugin" }
```

### 2) 루트 `build.gradle.kts` — plugins 블록에 추가(apply false)
```kotlin
alias(libs.plugins.google.services) apply false
alias(libs.plugins.firebase.crashlytics) apply false
```

### 3) `app/build.gradle.kts`
```kotlin
plugins {
    // ...기존...
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}
```

### 4) 무음 실패를 비치명 리포트로 승격
- `ImageStore.kt`의 `Log.w(...)` 지점(TODO(H1) 주석)에 다음을 추가:
```kotlin
com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(it)
```
- 동일하게 `CaptureRepository.ensureSummary` 등 온디바이스 AI 경로의 무음 실패도 `recordException`으로 계측.

### 5) 처리방침 반영
- `docs/PRIVACY_POLICY_ko.md`의 "2. 외부로 전송되는 정보" 항목이 이미 Crashlytics를 반영함. Play 데이터 안전 양식에 "진단 정보(익명)"로 신고.

## 검증
- 디버그 빌드에서 강제 크래시(`throw RuntimeException("test")`)를 한 번 발생 → Firebase 콘솔 Crashlytics에 수 분 내 표시되는지 확인 후 테스트 코드 제거.
