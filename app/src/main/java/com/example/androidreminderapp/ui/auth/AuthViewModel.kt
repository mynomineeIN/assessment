package com.example.androidreminderapp.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidreminderapp.domain.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    object Welcome : AuthUiState
    object SignUp : AuthUiState
    object PhoneInput : AuthUiState
    data class OtpVerification(val maskedPhone: String) : AuthUiState
    object AuthSuccess : AuthUiState
}

sealed interface AuthSessionState {
    object Loading : AuthSessionState
    object Authenticated : AuthSessionState
    object Unauthenticated : AuthSessionState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Welcome)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var verificationId: String = ""
        private set

    var resendToken: PhoneAuthProvider.ForceResendingToken? = null
        private set

    var storedPhoneNumber: String = ""
        private set

    var storedCountryCode: String = "+91"
        private set

    private var isAuthCompleted: Boolean = false

    val userDisplayInfo: String
        get() {
            val user = authRepository.currentUser
            return user?.phoneNumber ?: user?.uid ?: "Authenticated User"
        }

    init {
        viewModelScope.launch {
            authRepository.isUserAuthenticatedFlow.collect { isAuthenticated ->
                if (isAuthenticated) {
                    _sessionState.value = AuthSessionState.Authenticated
                } else {
                    _sessionState.value = AuthSessionState.Unauthenticated
                }
            }
        }
    }

    fun setAuthStep(state: AuthUiState) {
        if (isAuthCompleted && state is AuthUiState.AuthSuccess) return
        _uiState.value = state
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun requestOtp(countryCode: String, fullPhoneNumber: String, activity: Activity, onLoginSuccess: (() -> Unit)? = null) {
        if (isAuthCompleted) return
        if (fullPhoneNumber.isBlank() || fullPhoneNumber.length < 10) {
            _errorMessage.value = "Please enter a valid mobile number (at least 10 digits)."
            return
        }
        storedCountryCode = countryCode
        storedPhoneNumber = fullPhoneNumber
        val formattedNumber = "$countryCode$fullPhoneNumber"
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                authRepository.requestPhoneOtp(
                    phoneNumber = formattedNumber,
                    activity = activity,
                    onCodeSent = { vId, token ->
                        if (!isAuthCompleted) {
                            verificationId = vId
                            resendToken = token
                            _isLoading.value = false
                            _uiState.value = AuthUiState.OtpVerification(maskedPhone = "$countryCode *****${fullPhoneNumber.takeLast(4)}")
                        }
                    },
                    onAutoVerificationSuccess = {
                        if (!isAuthCompleted) {
                            isAuthCompleted = true
                            _isLoading.value = false
                            _errorMessage.value = null
                            _uiState.value = AuthUiState.AuthSuccess
                            onLoginSuccess?.invoke()
                        }
                    },
                    onVerificationFailed = { e ->
                        if (!isAuthCompleted) {
                            _isLoading.value = false
                            _errorMessage.value = e.localizedMessage ?: "Phone verification failed."
                        }
                    },
                    forceResendingToken = resendToken
                )
            } catch (e: Exception) {
                if (!isAuthCompleted) {
                    _isLoading.value = false
                    _errorMessage.value = e.localizedMessage ?: "Failed to send OTP."
                }
            }
        }
    }

    fun verifyOtp(smsCode: String, onSuccess: () -> Unit) {
        if (isAuthCompleted) return
        if (smsCode.length < 6) {
            _errorMessage.value = "Please enter a valid 6-digit OTP."
            return
        }
        if (verificationId.isBlank()) {
            _errorMessage.value = "Verification ID missing. Please request OTP again."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.verifyPhoneOtp(verificationId, smsCode)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    if (!isAuthCompleted) {
                        isAuthCompleted = true
                        _uiState.value = AuthUiState.AuthSuccess
                        onSuccess()
                    }
                },
                onFailure = { e ->
                    if (!isAuthCompleted) {
                        _errorMessage.value = e.localizedMessage ?: "Invalid verification code."
                    }
                }
            )
        }
    }

    fun signInWithGoogle(activity: Activity, onLoginSuccess: (() -> Unit)? = null) {
        if (isAuthCompleted) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activity)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    if (!isAuthCompleted) {
                        isAuthCompleted = true
                        _uiState.value = AuthUiState.AuthSuccess
                        onLoginSuccess?.invoke()
                    }
                },
                onFailure = { e ->
                    if (!isAuthCompleted) {
                        _errorMessage.value = e.localizedMessage ?: "Google Sign-In failed."
                    }
                }
            )
        }
    }

    fun signOut() {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                authRepository.signOut()
                isAuthCompleted = false
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Failed to sign out. Please try again."
            }
        }
    }
}
