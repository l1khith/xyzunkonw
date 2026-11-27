package com.splitsignal.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Polished Linear-inspired Settings dialog with grouped sections,
 * storage/download preferences, local alerts, accent cycling, and safe local data reset.
 */
@Composable
fun SettingsModal(
    defaultSaveDir: String,
    autoSaveFiles: Boolean,
    canPickDirectory: Boolean,
    onChangeSaveDir: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onCycleAccent: () -> Unit,
    onClearData: () -> Unit,
    onClose: () -> Unit,
) {
    val backdropInteraction = remember { MutableInteractionSource() }
    var localAlertsEnabled by remember { mutableStateOf(true) }
    var confirmingClearData by remember { mutableStateOf(false) }
    var localDataCleared by remember { mutableStateOf(false) }

    val modalScale = remember { Animatable(0.92f) }
    val modalAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            modalScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
            )
        }
        launch {
            modalAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD908090A))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onClose,
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .graphicsLayer {
                    scaleX = modalScale.value
                    scaleY = modalScale.value
                    alpha = modalAlpha.value
                }
                .clip(RoundedCornerShape(16.dp))
                .background(SplitSignalColors.SurfaceCard)
                .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SplitSignalColors.SurfaceTile)
                            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = SplitSignalColors.AccentIndigo,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "PREFERENCES",
                            style = LinearTextStyles.TabularLabel.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SplitSignalColors.AccentIndigo,
                            ),
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SplitSignalColors.TextPrimary,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Scrollable Grouped Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Section 1: STORAGE & DOWNLOADS
                Text(
                    text = "STORAGE & DOWNLOADS",
                    style = LinearTextStyles.TabularLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitSignalColors.TextSecondary,
                    ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Save Folder Row
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
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
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
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (canPickDirectory) {
                                    SplitSignalColors.TextPrimary
                                } else {
                                    SplitSignalColors.TextMuted
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = SplitSignalColors.BorderSubtle, thickness = 1.dp)

                    // Auto-Save Row
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
                                contentDescription = "Auto-Save",
                                tint = SplitSignalColors.AccentEmerald,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Auto-Save Received Files",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
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
                                checkedThumbColor = Color(0xFFF7F8F8),
                                checkedTrackColor = SplitSignalColors.AccentEmerald,
                                uncheckedThumbColor = SplitSignalColors.TextSecondary,
                                uncheckedTrackColor = SplitSignalColors.SurfaceCard,
                                uncheckedBorderColor = SplitSignalColors.BorderSubtle,
                            ),
                        )
                    }
                }

                // Section 2: PREFERENCES & SYSTEM
                Text(
                    text = "PREFERENCES & SYSTEM",
                    style = LinearTextStyles.TabularLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SplitSignalColors.TextSecondary,
                    ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Local Alerts Row
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
                                imageVector = Icons.Rounded.NotificationsNone,
                                contentDescription = "Local Alerts",
                                tint = SplitSignalColors.AccentEmerald,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Local Alerts",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
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
                                checkedThumbColor = Color(0xFFF7F8F8),
                                checkedTrackColor = SplitSignalColors.AccentEmerald,
                                uncheckedThumbColor = SplitSignalColors.TextSecondary,
                                uncheckedTrackColor = SplitSignalColors.SurfaceCard,
                                uncheckedBorderColor = SplitSignalColors.BorderSubtle,
                            ),
                        )
                    }

                    HorizontalDivider(color = SplitSignalColors.BorderSubtle, thickness = 1.dp)

                    // Accent Color Row
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
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Player Accent",
                                tint = SplitSignalColors.AccentIndigo,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Player Accent",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                    color = SplitSignalColors.TextPrimary,
                                )
                                Text(
                                    text = "Personal identity & highlight color",
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
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = SplitSignalColors.BorderSubtle, thickness = 1.dp)

                    // Clear Local Data Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                        color = SplitSignalColors.TextPrimary,
                                    )
                                    Text(
                                        text = if (localDataCleared) {
                                            "Local cache and history cleared"
                                        } else {
                                            "Reset cached peers and local chat"
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
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = SplitSignalColors.AccentCrimson,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                                    text = "Reset all cached data?",
                                    style = LinearTextStyles.TabularLabel.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SplitSignalColors.AccentCrimson,
                                    ),
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
                                            onClearData()
                                        },
                                        backgroundColor = SplitSignalColors.AccentCrimson,
                                        restingBorderColor = SplitSignalColors.AccentCrimson,
                                        cornerRadius = 8.dp,
                                    ) {
                                        Text(
                                            text = "Confirm Reset",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
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
}
