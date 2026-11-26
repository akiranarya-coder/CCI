package com.example.cci.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

suspend fun attemptAutoSignIn(
    context: Context,
    onSignedIn: (FirebaseUser) -> Unit,
    onNoSession: () -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.default_web_client_id)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                onSignedIn(user)
            } else {
                onNoSession()
            }
        } else {
            onNoSession()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: NoCredentialException) {
        Log.d("Auth", "No returning Google credential available; showing sign-in button")
        onNoSession()
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Silent Google auto sign-in cancelled by user", e)
        onNoSession()
    } catch (e: GetCredentialException) {
        Log.w("Auth", "Silent Google auto sign-in unavailable: ${e.message}")
        onNoSession()
    } catch (e: Exception) {
        Log.w("Auth", "Silent Google auto sign-in failed: ${e.message}")
        onNoSession()
    }
}

suspend fun onGoogleSignInClicked(
    context: Context,
    onLoadingChange: (Boolean) -> Unit,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    onLoadingChange(true)
    val credentialManager = CredentialManager.create(context)
    val webClientId = context.getString(R.string.default_web_client_id)

    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                onLoadingChange(false)
                onSuccess(user)
            } else {
                onLoadingChange(false)
                onError("Google Sign-In completed without a Firebase user.")
            }
        } else {
            onLoadingChange(false)
            onError("Unexpected credential type received from Google Sign-In.")
        }
    } catch (e: CancellationException) {
        onLoadingChange(false)
        throw e
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "User cancelled Google Sign-In", e)
        onLoadingChange(false)
    } catch (e: GoogleIdTokenParsingException) {
        Log.e("Auth", "Failed to parse Google ID token", e)
        onLoadingChange(false)
        onError("Unable to verify Google account credentials. Please try again.")
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Google Sign-In failed: ${e.type} - ${e.message}", e)
        onLoadingChange(false)
        onError(e.errorMessage?.toString() ?: "Google Sign-In failed. Check your network and try again.")
    } catch (e: Exception) {
        Log.e("Auth", "Firebase sign-in failed", e)
        onLoadingChange(false)
        onError(e.message ?: "Sign-in failed. Please try again.")
    }
}

suspend fun signOutGoogle(context: Context) {
    withContext(NonCancellable) {
        try {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state on sign-out", e)
        }
        Firebase.auth.signOut()
    }
}
