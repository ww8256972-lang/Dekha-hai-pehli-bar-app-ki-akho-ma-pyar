# GitHub Actions Workflow Setup Guide for ASLAM RECORD

Yeh project GitHub Actions ke sath fully configured hai. Jab aap is repository ko GitHub par push karenge, toh GitHub Actions workflow automatically chalega aur APK build kar dega.

---

## 1. Action Flow Files (Workflow Files)

Project mein do workflows banaye gaye hain:

1. **`.github/workflows/android.yml`**:
   - Har `git push` aur `pull request` par automatically run hota hai.
   - Project ko compile karta hai, unit tests check karta hai, aur **Debug APK** build karke GitHub Artifacts mein upload karta hai.
   - Agar release keystore configured ho toh **Release APK** bhi build karta hai.

2. **`.github/workflows/manual-build.yml`**:
   - GitHub ke **Actions** tab mein jakar aap **"Run workflow"** button daba kar kabhi bhi on-demand APK build kar sakte hain (Debug ya Release choose karke).

---

## 2. GitHub Secrets (Secret Variables) Kaise Set Karein

GitHub repository mein **Settings > Secrets and variables > Actions > New repository secret** par click karein:

| Secret Name | Zaroori Hai? | Description |
|---|---|---|
| `GEMINI_API_KEY` | Recommended | ASLAM AI ke liye Gemini API Key. Agar nahi set karenge toh bhi build fail nahi hoga, local calculation chalega. |
| `KEYSTORE_BASE64` | Optional | Signed Release APK ke liye `.jks` file ka base64 string. |
| `STORE_PASSWORD` | Optional | Release Keystore ka password (e.g. `android` ya aapka password). |
| `KEY_PASSWORD` | Optional | Keystore Key ka password. |

> **Note:** Agar aapne koi secret set nahi bhi kiya, tab bhi workflow **100% bina error ke chalega** aur **Debug APK** generate karke dega!

---

## 3. Project Configuration Files Check

Aapke prompt mein di gayi sari files verify aur create kar di gayi hain:

- ✅ **`AndroidManifest.xml`**: Permissions (`INTERNET`, `ACCESS_NETWORK_STATE`), `AslamRecordApp`, `MainActivity`, aur `FileProvider` configure hain.
- ✅ **`build.gradle.kts` (Project & App)**: CompileSdk 36, MinSdk 24, Compose, Room, Moshi, Retrofit, Secrets plugin sabhi up-to-date hain.
- ✅ **`gradle.properties`**: JVM args, parallel execution, caching, nonTransitiveRClass sab configured hain.
- ✅ **`settings.gradle.kts`**: Google, MavenCentral aur `rootProject.name = "ASLAM RECORD"`.
- ✅ **`MainActivity.kt`**: Jetpack Compose Scaffold, TopBar, BottomBar, fail-safe ViewModel init.
- ✅ **`activity_main.xml`**: Layout resource `app/src/main/res/layout/activity_main.xml`.
- ✅ **`strings.xml`**: App name `ASLAM RECORD` aur string resources.
- ✅ **`colors.xml` & `styles.xml`**: Color palette aur AppTheme.
- ✅ **`proguard-rules.pro`**: Room, Moshi, Retrofit aur Coroutines rules.
- ✅ **`gradlew` & `gradlew.bat`**: Executable wrapper scripts for GitHub CI.
