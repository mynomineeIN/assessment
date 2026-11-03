package com.example.androidreminderapp.data.repository

import com.example.androidreminderapp.domain.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    companion object {
        private const val TAG = "AuthDebug"
    }

    override val currentUser: FirebaseUser?
        get() = try {
            val user = firebaseAuth.currentUser
            android.util.Log.d(TAG, "Current authentication state checked. User logged in: ${user != null}")
            user
        } catch (_: Exception) {
            null
        }

    override val isUserAuthenticatedFlow: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser != null)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "Firebase signInWithCredential started")
            firebaseAuth.signInWithCredential(credential).await()
            android.util.Log.d(TAG, "Firebase signInWithCredential succeeded")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Firebase signInWithCredential failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signInWithCustomToken(token: String): Result<Boolean> {
        return try {
            firebaseAuth.signInWithCustomToken(token).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: android.app.Activity): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "Credential Manager request started for Google Sign-In")
            val credentialManager = androidx.credentials.CredentialManager.create(activity)
            val webClientId = activity.getString(com.example.androidreminderapp.R.string.default_web_client_id)
            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                android.util.Log.d(TAG, "Google credential received via Credential Manager")
                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                android.util.Log.d(TAG, "Firebase Google sign-in succeeded")
                Result.success(true)
            } else {
                android.util.Log.e(TAG, "Invalid Google Credential type received")
                Result.failure(Exception("Invalid Google Credential type"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Google Sign-In failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun requestPhoneOtp(
        phoneNumber: String,
        activity: android.app.Activity,
        onCodeSent: (verificationId: String, token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onAutoVerificationSuccess: () -> Unit,
        onVerificationFailed: (Exception) -> Unit,
        forceResendingToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?
    ) {
        try {
            android.util.Log.d(TAG, "OTP request started for phone number")
            val optionsBuilder = com.google.firebase.auth.PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                        android.util.Log.d(TAG, "Phone verification auto-completed")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                firebaseAuth.signInWithCredential(credential).await()
                                android.util.Log.d(TAG, "Firebase auto-sign-in succeeded")
                                withContext(Dispatchers.Main) {
                                    onAutoVerificationSuccess()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "Firebase auto-sign-in failed: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    onVerificationFailed(e)
                                }
                            }
                        }
                    }
                    override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                        android.util.Log.e(TAG, "OTP request failed: ${e.message}")
                        onVerificationFailed(e)
                    }
                    override fun onCodeSent(
                        verificationId: String,
                        token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                    ) {
                        android.util.Log.d(TAG, "OTP request callback received: onCodeSent. Verification ID stored successfully.")
                        onCodeSent(verificationId, token)
                    }
                })
            if (forceResendingToken != null) {
                optionsBuilder.setForceResendingToken(forceResendingToken)
            }
            com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "OTP request exception: ${e.message}")
            onVerificationFailed(e)
        }
    }

    override suspend fun verifyPhoneOtp(verificationId: String, smsCode: String): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "OTP credential creation started")
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, smsCode)
            android.util.Log.d(TAG, "OTP credential creation completed")
            firebaseAuth.signInWithCredential(credential).await()
            android.util.Log.d(TAG, "Firebase phone sign-in succeeded")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Firebase phone sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }

    override fun signOut() {
        try {
            firebaseAuth.signOut()
            android.util.Log.d(TAG, "Logout completed")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Logout failed: ${e.message}")
            throw e
        }
    }
}
