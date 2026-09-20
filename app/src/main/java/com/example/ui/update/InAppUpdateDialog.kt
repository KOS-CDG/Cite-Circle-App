package com.example.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.network.SupabaseClient
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue

@Composable
fun InAppUpdateDialog(
    updateInfo: SupabaseClient.AppUpdateInfo,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.mandatory) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.mandatory,
            dismissOnClickOutside = !updateInfo.mandatory
        ),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = "Update Icon",
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "New Update Ready",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Version ${updateInfo.latestVersionName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = AccentGreen, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A new release of Cite Circle is available with improvements and fixes:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (updateInfo.fileSizeMb > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Download size: ~${updateInfo.fileSizeMb} MB",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update Now", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (!updateInfo.mandatory) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    )
}
