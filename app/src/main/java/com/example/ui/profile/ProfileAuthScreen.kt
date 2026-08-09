package com.example.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileAuthScreen(viewModel: ProfileViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentUser == null) {
            Text("Not Signed In", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.signInWithGoogle(context) }) {
                Text("Sign in with Google")
            }
        } else {
            Text("Welcome, ${currentUser?.displayName ?: "User"}!", style = MaterialTheme.typography.headlineSmall)
            Text("Email: ${currentUser?.email}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Your Data from Firestore:", style = MaterialTheme.typography.titleMedium)
            Text(userData, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Update Note") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.saveUserData(inputText) }) {
                Text("Save to Firestore")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = { viewModel.signOut() }) {
                Text("Sign Out")
            }
        }
    }
}
