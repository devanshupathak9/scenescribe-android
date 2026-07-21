package com.scenescribe.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scenescribe.app.data.api.models.UserDto
import com.scenescribe.app.ui.components.*
import com.scenescribe.app.ui.theme.*

@Composable
fun AuthScreen(
    onAuthenticated: (UserDto, String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand header
            BrandHeader()

            Spacer(Modifier.height(32.dp))

            // Tab row (Register / Sign In) — shown on step 1 and sign-in
            if (state.step == AuthStep.Register1 || state.step == AuthStep.SignIn) {
                TabRow(
                    selectedTab = state.step,
                    onRegister = { viewModel.setStep(AuthStep.Register1) },
                    onSignIn = { viewModel.setStep(AuthStep.SignIn) }
                )
                Spacer(Modifier.height(28.dp))
            }

            // Step indicator for register flow
            if (state.step in listOf(AuthStep.Register1, AuthStep.Register2, AuthStep.Register3)) {
                StepIndicator(step = state.step)
                Spacer(Modifier.height(24.dp))
            }

            // Error
            if (state.error.isNotBlank()) {
                ErrorText(state.error)
                Spacer(Modifier.height(12.dp))
            }

            when (state.step) {
                AuthStep.Register1 -> RegisterStep1(state, viewModel)
                AuthStep.Register2 -> RegisterStep2(state, viewModel, onAuthenticated)
                AuthStep.Register3 -> RegisterStep3 {
                    val user  = viewModel.uiState.value
                    // Auth was already saved inside ViewModel; navigate up
                    // We re-read from TokenManager via MainActivity
                    onAuthenticated(
                        UserDto(id = "", userName = state.username, email = state.email, isAdmin = false),
                        ""
                    )
                }
                AuthStep.SignIn -> SignInStep(state, viewModel, onAuthenticated)
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SceneScribe",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Watch. Describe. Get fluent.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun TabRow(selectedTab: AuthStep, onRegister: () -> Unit, onSignIn: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(30.dp))
            .padding(4.dp)
    ) {
        TabButton(
            text = "Register",
            selected = selectedTab == AuthStep.Register1,
            onClick = onRegister,
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "Sign In",
            selected = selectedTab == AuthStep.SignIn,
            onClick = onSignIn,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Background else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StepIndicator(step: AuthStep) {
    val stepNum = when (step) {
        AuthStep.Register1 -> 1
        AuthStep.Register2 -> 2
        AuthStep.Register3 -> 3
        else -> 1
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { i ->
            val n = i + 1
            val done   = n < stepNum
            val active = n == stepNum
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done   -> Accent
                            active -> Accent
                            else   -> CardBorder
                        }
                    )
            )
            if (i < 2) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(if (done) Accent else CardBorder)
                )
            }
        }
    }
}

@Composable
private fun RegisterStep1(state: AuthUiState, viewModel: AuthViewModel) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Free forever. Start your first scene in under 2 minutes.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScTextField(
                value = state.firstName,
                onValueChange = { viewModel.updateField("firstName", it) },
                label = "First Name",
                placeholder = "Arjun",
                modifier = Modifier.weight(1f)
            )
            ScTextField(
                value = state.lastName,
                onValueChange = { viewModel.updateField("lastName", it) },
                label = "Last Name",
                placeholder = "Kumar",
                modifier = Modifier.weight(1f)
            )
        }

        ScTextField(
            value = state.email,
            onValueChange = { viewModel.updateField("email", it) },
            label = "Email Address",
            placeholder = "arjun@email.com"
        )

        ScTextField(
            value = state.username,
            onValueChange = { viewModel.updateField("username", it) },
            label = "Username",
            placeholder = "arjun_speaks"
        )

        Column {
            ScTextField(
                value = state.password,
                onValueChange = { viewModel.updateField("password", it) },
                label = "Password",
                placeholder = "Min. 8 characters",
                isPassword = true
            )
            Spacer(Modifier.height(8.dp))
            PasswordStrengthBar(strength = viewModel.passwordStrength(state.password))
        }

        ScButton(
            text = if (state.isLoading) "Creating…" else "Create account",
            onClick = { viewModel.register() },
            enabled = !state.isLoading
        )

        TextButton(
            onClick = { viewModel.setStep(AuthStep.SignIn) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? ", color = TextSecondary, fontSize = 13.sp)
            Text("Sign in", color = Accent, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PasswordStrengthBar(strength: Int) {
    val segColors = listOf(Danger, Warning, Warning, Success)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < strength) segColors[strength - 1] else CardBorder)
            )
        }
    }
}

@Composable
private fun RegisterStep2(
    state: AuthUiState,
    viewModel: AuthViewModel,
    onAuthenticated: (UserDto, String) -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Check your inbox", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Enter the 6-digit code sent to\n${state.email}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.otp.forEachIndexed { i, digit ->
                OtpBox(
                    digit = digit,
                    focusRequester = focusRequesters[i],
                    onValueChange = { value ->
                        if (value.length <= 1) {
                            viewModel.updateOtp(i, value)
                            if (value.isNotEmpty() && i < 5) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        } else {
                            // Handle paste
                            val digits = value.filter { it.isDigit() }.take(6)
                            digits.forEachIndexed { idx, c ->
                                if (idx < 6) viewModel.updateOtp(idx, c.toString())
                            }
                            focusRequesters[minOf(digits.length, 5)].requestFocus()
                        }
                    },
                    onBackspace = {
                        if (digit.isEmpty() && i > 0) {
                            focusRequesters[i - 1].requestFocus()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ScButton(
            text = if (state.isLoading) "Verifying…" else "Verify email",
            onClick = {
                viewModel.verify { user, token ->
                    // Step transitions handled inside VM; navigate on Register3
                }
            },
            enabled = !state.isLoading && state.otp.all { it.isNotEmpty() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.resendOtp() }) {
                Text(
                    text = if (state.resendCooldown > 0) "Resend in ${state.resendCooldown}s"
                           else "Resend code",
                    color = if (state.resendCooldown > 0) TextSecondary else Accent,
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = {
                viewModel.setStep(AuthStep.Register1)
            }) {
                Text("← Change email", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OtpBox(
    digit: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = digit,
        onValueChange = onValueChange,
        modifier = modifier
            .aspectRatio(1f)
            .focusRequester(focusRequester),
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        keyboardActions = KeyboardActions(
            onDone = { }
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = if (digit.isNotEmpty()) Accent.copy(0.5f) else CardBorder,
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            cursorColor = Accent
        )
    )
}

@Composable
private fun RegisterStep3(onGoDashboard: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Success.copy(alpha = 0.13f))
                .border(1.dp, Success.copy(alpha = 0.33f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Success, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Text("You're verified!", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Your email is confirmed. Welcome to SceneScribe — your first scene is ready and waiting.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        ScButton(text = "Go to dashboard →", onClick = onGoDashboard)
    }
}

@Composable
private fun SignInStep(
    state: AuthUiState,
    viewModel: AuthViewModel,
    onAuthenticated: (UserDto, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Your streak is waiting. Sign in and keep the momentum going.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(4.dp))

        ScTextField(
            value = state.signInEmail,
            onValueChange = { viewModel.updateField("signInEmail", it) },
            label = "Email Address",
            placeholder = "arjun@email.com"
        )

        ScTextField(
            value = state.signInPassword,
            onValueChange = { viewModel.updateField("signInPassword", it) },
            label = "Password",
            placeholder = "Your password",
            isPassword = true
        )

        ScButton(
            text = if (state.isLoading) "Signing in…" else "Sign in",
            onClick = { viewModel.signIn(onAuthenticated) },
            enabled = !state.isLoading
        )

        TextButton(
            onClick = { viewModel.setStep(AuthStep.Register1) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("No account yet? ", color = TextSecondary, fontSize = 13.sp)
            Text("Create one free", color = Accent, fontSize = 13.sp)
        }
    }
}
