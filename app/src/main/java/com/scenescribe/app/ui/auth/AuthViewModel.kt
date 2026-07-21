package com.scenescribe.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scenescribe.app.data.TokenManager
import com.scenescribe.app.data.api.ApiClient
import com.scenescribe.app.data.api.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AuthStep { Register1, Register2, Register3, SignIn }

data class AuthUiState(
    val step: AuthStep = AuthStep.Register1,
    // Register form
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val username: String = "",
    val password: String = "",
    // OTP
    val otp: List<String> = List(6) { "" },
    val resendCooldown: Int = 0,
    // Sign In
    val signInEmail: String = "",
    val signInPassword: String = "",
    // Shared
    val isLoading: Boolean = false,
    val error: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.create(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private var pendingUser: UserDto? = null
    private var pendingToken: String? = null

    fun setStep(step: AuthStep) {
        _uiState.value = _uiState.value.copy(step = step, error = "")
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "firstName"      -> _uiState.value.copy(firstName = value, error = "")
            "lastName"       -> _uiState.value.copy(lastName = value, error = "")
            "email"          -> _uiState.value.copy(email = value, error = "")
            "username"       -> _uiState.value.copy(username = value, error = "")
            "password"       -> _uiState.value.copy(password = value, error = "")
            "signInEmail"    -> _uiState.value.copy(signInEmail = value, error = "")
            "signInPassword" -> _uiState.value.copy(signInPassword = value, error = "")
            else -> _uiState.value
        }
    }

    fun updateOtp(index: Int, digit: String) {
        val current = _uiState.value.otp.toMutableList()
        current[index] = digit.filter { it.isDigit() }.take(1)
        _uiState.value = _uiState.value.copy(otp = current, error = "")
    }

    fun register(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.firstName.isBlank()) { setError("First name is required"); return }
        if (state.email.isBlank()) { setError("Email is required"); return }
        if (state.username.isBlank()) { setError("Username is required"); return }
        if (state.username.trim().length < 3) { setError("Username must be at least 3 characters"); return }
        if (state.password.isBlank()) { setError("Password is required"); return }
        if (state.password.length < 8) { setError("Password must be at least 8 characters"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = "")
            try {
                api.register(RegisterRequest(state.email.trim()))
                _uiState.value = _uiState.value.copy(
                    step = AuthStep.Register2,
                    resendCooldown = 30,
                    isLoading = false
                )
                startResendCountdown()
                onSuccess()
            } catch (e: Exception) {
                setError(e.message ?: "Registration failed")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun verify(onSuccess: (UserDto, String) -> Unit) {
        val state = _uiState.value
        val code = state.otp.joinToString("")
        if (code.length < 6) { setError("Please enter the full 6-digit code"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = "")
            try {
                val res = api.verify(
                    VerifyRequest(
                        email    = state.email.trim(),
                        otp      = code,
                        userName = state.username.trim(),
                        password = state.password
                    )
                )
                val user  = res.data?.user  ?: throw Exception("Invalid server response")
                val token = res.data.token
                pendingUser  = user
                pendingToken = token
                tokenManager.saveAuth(token, user)
                _uiState.value = _uiState.value.copy(step = AuthStep.Register3, isLoading = false)
                onSuccess(user, token)
            } catch (e: Exception) {
                setError(e.message ?: "Verification failed")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun resendOtp() {
        if (_uiState.value.resendCooldown > 0) return
        viewModelScope.launch {
            try {
                api.register(RegisterRequest(_uiState.value.email.trim()))
                _uiState.value = _uiState.value.copy(
                    otp = List(6) { "" },
                    resendCooldown = 30,
                    error = ""
                )
                startResendCountdown()
            } catch (e: Exception) {
                setError(e.message ?: "Failed to resend OTP")
            }
        }
    }

    fun signIn(onSuccess: (UserDto, String) -> Unit) {
        val state = _uiState.value
        if (state.signInEmail.isBlank()) { setError("Email is required"); return }
        if (state.signInPassword.isBlank()) { setError("Password is required"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = "")
            try {
                val res = api.login(LoginRequest(state.signInEmail.trim(), state.signInPassword))
                val user  = res.data?.user  ?: throw Exception("Invalid server response")
                val token = res.data.token
                tokenManager.saveAuth(token, user)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(user, token)
            } catch (e: Exception) {
                setError(e.message ?: "Sign in failed")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun startResendCountdown() {
        viewModelScope.launch {
            while (_uiState.value.resendCooldown > 0) {
                kotlinx.coroutines.delay(1000)
                _uiState.value = _uiState.value.copy(
                    resendCooldown = _uiState.value.resendCooldown - 1
                )
            }
        }
    }

    private fun setError(msg: String) {
        _uiState.value = _uiState.value.copy(error = msg)
    }

    fun passwordStrength(password: String): Int {
        var strength = 0
        if (password.length >= 8) strength++
        if (password.any { it.isUpperCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        if (password.any { !it.isLetterOrDigit() }) strength++
        return strength
    }
}
