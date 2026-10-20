# How to Build Signed Release APK via GitHub Actions

This repository is fully configured with automated CI/CD via GitHub Actions to build a Signed Release APK without requiring manual build tools or Termux.

## 1. Generate Keystore (.jks)
In Android Studio:
1. Go to **Build** -> **Generate Signed Bundle / APK** -> Select **APK**.
2. Click **Create new...** under Key store path.
3. Choose a path (e.g. `my-release-key.jks`), enter passwords for Key Store and Key, set an Alias (e.g., `upload` or `releasekey`), and fill in basic certificate details.
4. Click **OK**.

Alternatively, via command line:
```bash
keytool -genkeypair -v -keystore my-release-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

## 2. Convert Keystore to Base64
Encode your `.jks` file to Base64:
- **Linux / macOS**:
  ```bash
  base64 -w 0 my-release-key.jks > keystore_base64.txt
  ```
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Out-File -Encoding utf8 keystore_base64.txt
  ```

## 3. Add GitHub Repository Secrets
Go to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions** -> Click **New repository secret**:

| Secret Name | Description / Value |
| --- | --- |
| `KEYSTORE_BASE64` | The entire base64 string from `keystore_base64.txt` |
| `KEYSTORE_PASSWORD` | Password you set for the keystore file |
| `KEY_ALIAS` | Alias name for the key (e.g. `upload`) |
| `KEY_PASSWORD` | Password for the key alias |
| `GEMINI_API_KEY` | *(Optional)* Your Google Gemini API Key for Aslam AI |

## 4. Automatic Build & Download
- Whenever you push changes to `main` or `master`, the workflow **Release APK Build** (`release-apk.yml`) runs automatically.
- You can also trigger it manually: go to **Actions** tab -> **Release APK Build** -> **Run workflow**.
- When the job completes, download `app-release.apk` directly from the **Artifacts** section at the bottom of the workflow run page!
