package com.splitsignal.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen Linear-inspired Settings modal with storage/download preferences,
 * local alerts toggle, accent color cycling, and two-step local data clear confirmation.
 */
@Composable
fun SettingsModal(
    defaultSaveDir: String,
    autoSaveFiles: Boolean,
    canPickDirectory: Boolean,
    onChangeSaveDir: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onCycleAccent: () -> Unit,
    onClose: () -> Unit,
) {
    val backdropInteraction = remember { MutableInteractionSource() }
    var localAlertsEnabled by remember { mutableStateOf(true) }
    var confirmingClearData by remember { mutableStateOf(false) }
    var localDataCleared by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas.copy(alpha = 0.88f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onClose,
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearCard(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            borderColor = SplitSignalColors.BorderSubtle,
            contentPadding = 20.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(SplitSignalColors.SurfaceTile)
                            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = SplitSignalColors.AccentIndigo,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "PREFERENCES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SplitSignalColors.AccentIndigo,
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = SplitSignalColors.TextPrimary,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, CircleShape)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close settings",
                        tint = SplitSignalColors.TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // STORAGE & DOWNLOADS
                Text(
                    text = "STORAGE & DOWNLOADS",
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                    color = SplitSignalColors.TextSecondary,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Save Folder",
                                tint = SplitSignalColors.AccentIndigo,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Save Folder",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SplitSignalColors.TextPrimary,
                                )
                                Text(
                                    text = defaultSaveDir.ifBlank { "~/Downloads/SplitSignal" },
                                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                    color = SplitSignalColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        TactileTile(
                            onClick = onChangeSaveDir,
                            enabled = canPickDirectory,
                            backgroundColor = if (canPickDirectory) {
                                SplitSignalColors.SurfaceCard
                            } else {
                                SplitSignalColors.BgCanvas
                            },
                            restingBorderColor = SplitSignalColors.BorderSubtle,
                            cornerRadius = 8.dp,
                        ) {
                            Text(
                                text = "Change Folder",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (canPickDirectory) {
                                    SplitSignalColors.TextPrimary
                                } else {
                                    SplitSignalColors.TextMuted
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Auto-Save Received Files",
                                tint = SplitSignalColors.AccentEmerald,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Auto-Save Received Files",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SplitSignalColors.TextPrimary,
                                )
                                Text(
                                    text = "Save incoming LAN files automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SplitSignalColors.TextSecondary,
                                )
                            }
                        }

                        Switch(
                            checked = autoSaveFiles,
                            onCheckedChange = onToggleAutoSave,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SplitSignalColors.TextPrimary,
                                checkedTrackColor = SplitSignalColors.AccentEmerald,
                                uncheckedThumbColor = SplitSignalColors.TextSecondary,
                                uncheckedTrackColor = SplitSignalColors.SurfaceCard,
                                uncheckedBorderColor = SplitSignalColors.BorderSubtle,
                            ),
                        )
                    }
                }

                // Local Alerts Toggle (#26B57B)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = "Local Alerts",
                            tint = SplitSignalColors.AccentEmerald,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Local Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = "Sound & banner for LAN invites",
                                style = MaterialTheme.typography.bodySmall,
                                color = SplitSignalColors.TextSecondary,
                            )
                        }
                    }

                    Switch(
                        checked = localAlertsEnabled,
                        onCheckedChange = { localAlertsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SplitSignalColors.TextPrimary,
                            checkedTrackColor = SplitSignalColors.AccentEmerald,
                            uncheckedThumbColor = SplitSignalColors.TextSecondary,
                            uncheckedTrackColor = SplitSignalColors.SurfaceCard,
                            uncheckedBorderColor = SplitSignalColors.BorderSubtle,
                        ),
                    )
                }

                // Accent Color Cycle (#5E6AD2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Accent Color",
                            tint = SplitSignalColors.AccentIndigo,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.titleSmall,
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = "Cycle board highlight palette",
                                style = MaterialTheme.typography.bodySmall,
                                color = SplitSignalColors.TextSecondary,
                            )
                        }
                    }

                    TactileTile(
                        onClick = onCycleAccent,
                        backgroundColor = SplitSignalColors.AccentIndigo,
                        restingBorderColor = SplitSignalColors.AccentIndigo,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Cycle",
                            style = MaterialTheme.typography.labelMedium,
                            color = SplitSignalColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }

                // Clear Local Data (#E5484D with confirmation state)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(
                            1.dp,
                            if (confirmingClearData) {
                                SplitSignalColors.AccentCrimson.copy(alpha = 0.55f)
                            } else {
                                SplitSignalColors.BorderSubtle
                            },
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear Local Data",
                                tint = SplitSignalColors.AccentCrimson,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Clear Local Data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SplitSignalColors.TextPrimary,
                                )
                                Text(
                                    text = if (localDataCleared) {
                                        "Local cache cleared"
                                    } else {
                                        "Reset cached peers and session state"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (localDataCleared) {
                                        SplitSignalColors.AccentEmerald
                                    } else {
                                        SplitSignalColors.TextSecondary
                                    },
                                )
                            }
                        }

                        if (!confirmingClearData) {
                            TactileTile(
                                onClick = {
                                    localDataCleared = false
                                    confirmingClearData = true
                                },
                                backgroundColor = SplitSignalColors.AccentCrimson.copy(alpha = 0.14f),
                                restingBorderColor = SplitSignalColors.AccentCrimson.copy(alpha = 0.45f),
                                cornerRadius = 8.dp,
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SplitSignalColors.AccentCrimson,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }

                    if (confirmingClearData) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Confirm local reset?",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                color = SplitSignalColors.AccentCrimson,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TactileTile(
                                    onClick = { confirmingClearData = false },
                                    backgroundColor = SplitSignalColors.SurfaceCard,
                                    restingBorderColor = SplitSignalColors.BorderSubtle,
                                    cornerRadius = 8.dp,
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SplitSignalColors.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                                TactileTile(
                                    onClick = {
                                        confirmingClearData = false
                                        localDataCleared = true
                                    },
                                    backgroundColor = SplitSignalColors.AccentCrimson,
                                    restingBorderColor = SplitSignalColors.AccentCrimson,
                                    cornerRadius = 8.dp,
                                ) {
                                    Text(
                                        text = "Confirm",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF7F8F8),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
