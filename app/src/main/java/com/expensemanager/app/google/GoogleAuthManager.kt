package com.expensemanager.app.google

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

data class GoogleSignInParseResult(
    val account: GoogleSignInAccount?,
    val error: String?
)

class GoogleAuthManager(private val context: Context) {
    private val driveScope = Scope(DriveScopes.DRIVE_FILE)

    val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        GoogleSignIn.getClient(context, options)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return if (GoogleSignIn.hasPermissions(account, driveScope)) account else null
    }

    fun getLastAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    fun signOut() {
        signInClient.signOut()
    }

    fun accountFromSignInResult(data: Intent?): GoogleSignInAccount? {
        return runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        }.getOrNull()
    }

    fun parseSignInResult(data: Intent?): GoogleSignInParseResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            GoogleSignInParseResult(account = account, error = null)
        } catch (e: ApiException) {
            GoogleSignInParseResult(
                account = null,
                error = "ApiException code=${e.statusCode} message=${e.localizedMessage ?: "n/a"}"
            )
        } catch (e: Exception) {
            GoogleSignInParseResult(
                account = null,
                error = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}

