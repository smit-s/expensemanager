# Google OAuth + Drive Setup (com.smit.expensemanager)

Use these exact values for debug setup.

## App Identity
- Package name: `com.smit.expensemanager`
- Debug SHA-1: `41:C1:F8:BD:3E:54:CF:18:87:D5:D9:F1:6A:4F:C7:37:EF:F9:96:CC`
- Debug SHA-256: `6D:DD:20:56:58:45:F1:EC:B6:C1:7E:20:EC:48:A6:49:A7:04:32:FD:7D:B3:25:59:C1:77:05:8A:83:E8:1D:9B`

## Google Cloud Steps
1. Create/select a Google Cloud project.
2. Enable APIs:
   - Google Drive API
3. Configure OAuth consent screen:
   - User type: External (or Internal for Workspace)
   - Add test users (your Google account while testing)
   - Add scope: `.../auth/drive.appdata`
4. Create OAuth Client ID -> Android:
   - Package name: `com.smit.expensemanager`
   - SHA-1: `41:C1:F8:BD:3E:54:CF:18:87:D5:D9:F1:6A:4F:C7:37:EF:F9:96:CC`
5. (Recommended) Create another Android OAuth client for release with release SHA-1.

## Release SHA-1
When you have your release keystore:

```powershell
keytool -list -v -keystore <path-to-release.jks> -alias <your-alias>
```

If publishing with Play App Signing, also add Play Console App Signing SHA-1 as a separate OAuth Android client.

## In-App Flow
- More tab -> `Sign in`
- Settings -> `Backup to Google` / `Restore from Google`

## Troubleshooting
- `DEVELOPER_ERROR (10)`: package/SHA-1 mismatch in OAuth client.
- Sign-in works but Drive fails: Drive API or scope/consent screen not configured correctly.
- Works on debug but not release: release SHA-1 OAuth client missing.
