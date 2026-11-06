package com.example.androidreminderapp

import android.app.Activity
import com.example.androidreminderapp.domain.repository.AuthRepository
import com.example.androidreminderapp.ui.auth.AuthSessionState
import com.example.androidreminderapp.ui.auth.AuthUiState
import com.example.androidreminderapp.ui.auth.AuthViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyOrInvalidPhoneNumberValidation() {
        viewModel.requestOtp("+91", "", mockActivity())
        assertEquals("Please enter a valid mobile number (at least 10 digits).", viewModel.errorMessage.value)

        viewModel.requestOtp("+91", "12345", mockActivity())
        assertEquals("Please enter a valid mobile number (at least 10 digits).", viewModel.errorMessage.value)
    }

    @Test
    fun testPhoneNumberIsNormalizedToE164() {
        viewModel.requestOtp("+1", "(415) 555-2671", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("+14155552671", fakeRepository.requestedPhoneNumber)
    }

    @Test
    fun testPhoneAuthConfigurationErrorExplainsFirebaseSetup() {
        fakeRepository.requestOtpFailure =
            Exception("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]")

        viewModel.requestOtp("+91", "9876543210", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Firebase Phone Authentication is not configured for this app. Enable the Phone provider, allow SMS to the user's country, add this app's SHA-1 and SHA-256 fingerprints in Firebase, and check that API-key restrictions allow notify-68514.firebaseapp.com.",
            viewModel.errorMessage.value
        )
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun testGoogleSignInSuccess() {
        var successCalled = false
        viewModel.signInWithGoogle(mockActivity()) { successCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun testGoogleSignInFailure() {
        fakeRepository.shouldFailGoogleSignIn = true
        viewModel.signInWithGoogle(mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
        assertNotEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)
    }

    @Test
    fun testInvalidOrEmptyOtpValidation() {
        // Without verificationId, verifyOtp fails
        viewModel.verifyOtp("123456", {})
        assertEquals("Verification ID missing. Please request OTP again.", viewModel.errorMessage.value)

        // With verificationId but short OTP
        viewModel.requestOtp("+91", "9876543210", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyOtp("123", {})
        assertEquals("Please enter a valid 6-digit OTP.", viewModel.errorMessage.value)

        viewModel.verifyOtp("12ab56", {})
        assertEquals("Please enter a valid 6-digit OTP.", viewModel.errorMessage.value)

        viewModel.verifyOtp("1234567", {})
        assertEquals("Please enter a valid 6-digit OTP.", viewModel.errorMessage.value)
    }

    @Test
    fun testSuccessfulVerificationStateTransition() {
        viewModel.requestOtp("+91", "9876543210", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.OtpVerification)

        var successCalled = false
        viewModel.verifyOtp("123456") {
            successCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)
    }

    @Test
    fun testFailedVerificationStateTransition() {
        fakeRepository.shouldFailVerification = true
        viewModel.requestOtp("+91", "9876543210", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyOtp("000000", {})
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun testAutoVerificationSuccess() {
        fakeRepository.triggerAutoVerification = true
        var successCalled = false
        viewModel.requestOtp("+91", "9876543210", mockActivity()) {
            successCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun testAutoVerificationFailure() {
        fakeRepository.triggerAutoVerification = true
        fakeRepository.shouldFailAutoVerification = true
        viewModel.requestOtp("+91", "9876543210", mockActivity())
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
        assertNotEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)
    }

    @Test
    fun testDuplicateCompletionProtection() {
        fakeRepository.triggerAutoVerification = true
        var callbackCount = 0
        viewModel.requestOtp("+91", "9876543210", mockActivity()) {
            callbackCount++
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, callbackCount)
        assertEquals(AuthUiState.AuthSuccess, viewModel.uiState.value)

        // Attempting another verify or request should be ignored due to isAuthCompleted
        viewModel.verifyOtp("123456", {})
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, callbackCount)
    }

    @Test
    fun testSessionStateRestorationAuthenticated() = runTest {
        fakeRepository.emitAuthenticated(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthSessionState.Authenticated, viewModel.sessionState.value)
    }

    @Test
    fun testSessionStateUnauthenticated() = runTest {
        fakeRepository.emitAuthenticated(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthSessionState.Unauthenticated, viewModel.sessionState.value)
    }

    @Test
    fun testLogoutStateTransition() = runTest {
        fakeRepository.emitAuthenticated(true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthSessionState.Authenticated, viewModel.sessionState.value)

        viewModel.signOut()
        fakeRepository.emitAuthenticated(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthSessionState.Unauthenticated, viewModel.sessionState.value)
    }
}

class FakeAuthRepository : AuthRepository {
    var shouldFailVerification = false
    var shouldFailAutoVerification = false
    var shouldFailGoogleSignIn = false
    var triggerAutoVerification = false
    var requestOtpFailure: Exception? = null
    var requestedPhoneNumber: String? = null
    private val _isAuthenticated = MutableStateFlow(false)

    override val currentUser: FirebaseUser? = null

    override val isUserAuthenticatedFlow: Flow<Boolean> = _isAuthenticated

    fun emitAuthenticated(isAuthenticated: Boolean) {
        _isAuthenticated.value = isAuthenticated
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<Boolean> {
        return if (shouldFailVerification) Result.failure(Exception("Verification failed"))
        else Result.success(true)
    }

    override suspend fun signInWithCustomToken(token: String): Result<Boolean> = Result.success(true)

    override suspend fun signInWithGoogle(activity: Activity): Result<Boolean> =
        if (shouldFailGoogleSignIn) Result.failure(Exception("Google Sign-In failed"))
        else Result.success(true)

    override suspend fun requestPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onAutoVerificationSuccess: () -> Unit,
        onVerificationFailed: (Exception) -> Unit,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken?
    ) {
        requestedPhoneNumber = phoneNumber
        requestOtpFailure?.let {
            onVerificationFailed(it)
            return
        }
        if (triggerAutoVerification) {
            if (shouldFailAutoVerification) {
                onVerificationFailed(Exception("Auto-verification failed"))
            } else {
                onAutoVerificationSuccess()
            }
        } else {
            onCodeSent("fake-verification-id", null)
        }
    }

    override suspend fun verifyPhoneOtp(verificationId: String, smsCode: String): Result<Boolean> {
        return if (shouldFailVerification) Result.failure(Exception("Invalid code"))
        else Result.success(true)
    }

    override fun signOut() {
        _isAuthenticated.value = false
    }
}

private fun mockActivity(): Activity {
    return Activity()
}
