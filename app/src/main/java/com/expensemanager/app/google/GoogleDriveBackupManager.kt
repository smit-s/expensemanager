package com.expensemanager.app.google

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class GoogleDriveBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val backupFileName = "expense_manager_backup.bk"

    private fun driveFor(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(appContext, setOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account
        return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Expense Manager")
            .build()
    }

    fun uploadBackup(account: GoogleSignInAccount, sourceFile: java.io.File): String {
        val drive = driveFor(account)
        val existingId = drive.files().list()
            .setQ("name = '$backupFileName' and trashed = false")
            .setFields("files(id,name)")
            .execute()
            .files
            ?.firstOrNull()
            ?.id

        val meta = File().apply {
            name = backupFileName
            mimeType = "application/octet-stream"
        }
        val media = FileContent("application/octet-stream", sourceFile)

        return if (existingId == null) {
            drive.files().create(meta, media).setFields("id").execute().id ?: "created"
        } else {
            drive.files().update(existingId, meta, media).setFields("id").execute().id ?: "updated"
        }
    }

    fun restoreBackup(account: GoogleSignInAccount, targetFile: java.io.File): Boolean {
        val drive = driveFor(account)
        val item = drive.files().list()
            .setQ("name = '$backupFileName' and trashed = false")
            .setFields("files(id,name)")
            .execute()
            .files
            ?.firstOrNull()
            ?: return false

        val out = ByteArrayOutputStream()
        drive.files().get(item.id).executeMediaAndDownloadTo(out)
        FileOutputStream(targetFile).use { it.write(out.toByteArray()) }
        return true
    }
}


