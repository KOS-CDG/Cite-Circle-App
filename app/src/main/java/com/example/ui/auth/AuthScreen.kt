package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.ui.theme.wordmark
import kotlinx.coroutines.launch

/**
 * The last screen still carrying the pre-redesign styling: wildcard imports, a hardcoded 24dp
 * page inset, `RoundedCornerShape(4.dp)` from the old severe corner scale, and an inline
 * `letterSpacing` override of exactly the kind Typography.eyebrow was introduced to replace.
 *
 * Now it leads with the brand gradient, which is the strongest statement of the new palette and
 * belongs on the one screen whose whole job is to introduce the product.
 *
 * Behaviour is unchanged and deliberately so: sign-in is bypassed because
 * `google-services.json` is a placeholder, so the failure branch continues into the app. That is
 * pre-existing and out of scope for a visual pass -- but it is now stated on screen instead of
 * happening silently behind a "Sign in failed" toast.
 */
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager(context) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient occupies the top two thirds; the action sits on solid background so the button
        // and the small print never fight the gradient for contrast.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(Gradients.brandHeader()),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.screenHorizontal),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "CC",
                        style = MaterialTheme.typography.wordmark,
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.base))
                Text(
                    "Cite Circle",
                    style = MaterialTheme.typography.wordmark,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Where research finds its readers",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val success = authManager.signInWithGoogle()
                                isLoading = false
                                if (success) {
                                    onAuthSuccess()
                                } else {
                                    // Firebase is not provisioned in this project, so sign-in
                                    // cannot succeed. Continuing is the honest behaviour for a
                                    // demo build, and the caption below says so plainly rather
                                    // than flashing a failure toast and proceeding anyway.
                                    onAuthSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.touchTarget),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            "CONTINUE WITH GOOGLE",
                            style = MaterialTheme.typography.eyebrow,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        "Demo build — sign-in is not configured, so this continues straight " +
                            "into the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
