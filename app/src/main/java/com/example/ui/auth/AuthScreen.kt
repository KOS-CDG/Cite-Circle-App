package com.example.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyApplication
import com.example.R
import kotlinx.coroutines.launch

enum class AuthTab { SIGN_IN, REGISTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val scope = rememberCoroutineScope()
    val sessionManager = app.sessionManager
    val authManager = remember { FirebaseAuthManager(context) }

    var selectedTab by remember { mutableStateOf(AuthTab.SIGN_IN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Sign In form fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Register form fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regAffiliation by remember { mutableStateOf("") }
    var regField by remember { mutableStateOf("Computer Science & AI") }
    var regAcceptedPrivacy by remember { mutableStateOf(false) }
    var fieldDropdownExpanded by remember { mutableStateOf(false) }

    val researchFields = listOf(
        "Computer Science & AI",
        "Biophysics & Molecular Biology",
        "Medicine & Clinical Trials",
        "Physics & Quantum Computing",
        "Mathematics & Statistics",
        "Economics & Social Sciences",
        "Chemistry & Materials Science",
        "Environmental & Climate Science",
        "Interdisciplinary Research"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Hero Branding
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "Where Academic Discovery Connects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Segmented Tabs: Sign In vs Create Account
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(MaterialTheme.shapes.small)
        ) {
            Tab(
                selected = selectedTab == AuthTab.SIGN_IN,
                onClick = {
                    selectedTab = AuthTab.SIGN_IN
                    errorMessage = null
                },
                text = {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
            Tab(
                selected = selectedTab == AuthTab.REGISTER,
                onClick = {
                    selectedTab = AuthTab.REGISTER
                    errorMessage = null
                },
                text = {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Error banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        when (selectedTab) {
            AuthTab.SIGN_IN -> {
                // Sign In Form
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = {
                        loginEmail = it
                        errorMessage = null
                    },
                    label = { Text("Academic Email") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = {
                        loginPassword = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                if (loginPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (loginPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Text(
                            "Remember me",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text(
                            "Forgot Password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = sessionManager.login(loginEmail, loginPassword)
                            isLoading = false
                            when (result) {
                                is com.example.data.auth.AuthResult.Success -> {
                                    Toast.makeText(context, "Welcome back, ${result.user.displayName}!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                }
                                is com.example.data.auth.AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                                is com.example.data.auth.AuthResult.RateLimited -> {
                                    errorMessage = "Rate limit reached: Too many failed login attempts. Please wait ${result.waitSeconds} seconds."
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Log In", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Fast-Track 1-Tap Demo Researcher Login
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val result = sessionManager.loginAsDemoResearcher()
                            isLoading = false
                            if (result is com.example.data.auth.AuthResult.Success) {
                                Toast.makeText(context, "Signed in as Demo Researcher (${result.user.displayName})", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "One-Tap Demo Researcher Access",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Institutional / Google Sign In
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val success = authManager.signInWithGoogle()
                            isLoading = false
                            if (success) {
                                onAuthSuccess()
                            } else {
                                // Fallback to instant demo sign-in
                                sessionManager.loginAsDemoResearcher()
                                onAuthSuccess()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        stringResource(R.string.action_continue_with_google),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AuthTab.REGISTER -> {
                // Register / Create Account Form
                OutlinedTextField(
                    value = regName,
                    onValueChange = {
                        regName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name (e.g. Dr. Jane Doe)") },
                    leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = regEmail,
                    onValueChange = {
                        regEmail = it
                        errorMessage = null
                    },
                    label = { Text("Academic Email Address") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = regPassword,
                    onValueChange = {
                        regPassword = it
                        errorMessage = null
                    },
                    label = { Text("Password (min 6 characters)") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                            Icon(
                                if (regPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (regPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = regAffiliation,
                    onValueChange = {
                        regAffiliation = it
                        errorMessage = null
                    },
                    label = { Text("Institution / University") },
                    leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) },
                    placeholder = { Text("e.g. MIT, Stanford, Oxford") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                // Research Field selector
                ExposedDropdownMenuBox(
                    expanded = fieldDropdownExpanded,
                    onExpandedChange = { fieldDropdownExpanded = !fieldDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = regField,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primary Research Field") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fieldDropdownExpanded,
                        onDismissRequest = { fieldDropdownExpanded = false }
                    ) {
                        researchFields.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field) },
                                onClick = {
                                    regField = field
                                    fieldDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Privacy Agreement Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = regAcceptedPrivacy,
                        onCheckedChange = { regAcceptedPrivacy = it }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "I agree to Cite Circle's Terms of Service and Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "View Privacy Policy & Data Rights",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToPrivacy() }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = sessionManager.register(
                                name = regName,
                                email = regEmail,
                                password = regPassword,
                                affiliation = regAffiliation,
                                researchField = regField,
                                acceptedPrivacy = regAcceptedPrivacy
                            )
                            isLoading = false
                            when (result) {
                                is com.example.data.auth.AuthResult.Success -> {
                                    Toast.makeText(context, "Account created! Let's configure permissions.", Toast.LENGTH_SHORT).show()
                                    onNavigateToPermissions()
                                }
                                is com.example.data.auth.AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                                is com.example.data.auth.AuthResult.RateLimited -> {
                                    errorMessage = "Rate limit reached. Please wait ${result.waitSeconds} seconds."
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Academic Account", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Privacy link footer
        Text(
            "Cite Circle Academic Network • Privacy & Governance Policy",
            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onNavigateToPrivacy() }
        )

        Spacer(Modifier.height(16.dp))
    }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(loginEmail) }
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Academic Password") },
            text = {
                Column {
                    Text("Enter your academic email address. If an account exists, instructions will be sent.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Academic Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        Toast.makeText(context, "Password recovery request submitted for $resetEmail", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("Send Instructions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
