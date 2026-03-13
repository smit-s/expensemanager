# Expense Manager (Android)

## Build APK

### 1. Open terminal in project root
```powershell
cd %USERPROFILE%\expensemanager
```

### 2. Set Java (Android Studio bundled JBR)
```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

### 3. Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```

Debug APK output:
`app\build\outputs\apk\debug\app-debug.apk`

### 4. Build Release APK
```powershell
.\gradlew.bat assembleRelease
```

Release APK output:
`app\build\outputs\apk\release\app-release.apk`

## Build App Bundle (for Play Store)

```powershell
.\gradlew.bat bundleRelease
```

Bundle output:
`app\build\outputs\bundle\release\app-release.aab`

## Install and Run on Emulator

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb -s emulator-5554 install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb -s emulator-5554 shell monkey -p com.expensemanager.app -c android.intent.category.LAUNCHER 1
```

If your emulator id is different, replace `emulator-5554` with your actual device id from `adb devices -l`.
