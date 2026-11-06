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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
            firebaseAuth.currentUser?.let(::saveUserProfile)
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
            firebaseAuth.currentUser?.let(::saveUserProfile)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: android.app.Activity): Result<Boolean> {
        return try {
            val webClientIdResource = activity.resources.getIdentifier(
                "default_web_client_id",
                "string",
                activity.packageName
            )
            if (webClientIdResource == 0) {
                return Result.failure(
                    Exception("Google Sign-In is not configured. Add a Web OAuth client in Firebase.")
                )
            }
            val credentialManager = androidx.credentials.CredentialManager.create(activity)
            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(webClientIdResource))
                .setAutoSelectEnabled(false)
                .build()
            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential !is androidx.credentials.CustomCredential ||
                credential.type != com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(Exception("Google Sign-In returned an unsupported credential."))
            }

            val googleCredential =
                com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(
                googleCredential.idToken,
                null
            )
            firebaseAuth.signInWithCredential(firebaseCredential).await()
            firebaseAuth.currentUser?.let(::saveUserProfile)
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Google Sign-In failed", e)
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
                                firebaseAuth.currentUser?.let(::saveUserProfile)
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
            firebaseAuth.currentUser?.let(::saveUserProfile)
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

    private fun saveUserProfile(user: FirebaseUser) {
        val profile = mapOf(
            "uid" to user.uid,
            "phoneNumber" to user.phoneNumber,
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl?.toString(),
            "provider" to user.providerData
                .firstOrNull { it.providerId != "firebase" }
                ?.providerId,
            "createdAt" to (user.metadata?.creationTimestamp ?: System.currentTimeMillis()),
            "lastSignInAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users")
            .document(user.uid)
            .set(profile, SetOptions.merge())
            .addOnFailureListener { error ->
                android.util.Log.e(TAG, "Failed to save authenticated user profile to Firestore", error)
            }
    }
}
