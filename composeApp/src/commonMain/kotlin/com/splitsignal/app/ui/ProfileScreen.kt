package com.splitsignal.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.CompetenceTrophy
import com.splitsignal.core.domain.FriendRecord
import com.splitsignal.core.domain.PrivacyMode
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.UserIdentity
import kotlinx.coroutines.delay

private val AvatarAccentPalette: List<Color> = listOf(
    SplitSignalColors.AccentIndigo,
    SplitSignalColors.AccentEmerald,
    SplitSignalColors.AccentAmber,
    Color(0xFF38BDF8),
    Color(0xFFA78BFA),
    SplitSignalColors.AccentCrimson,
)

/**
 * Linear-inspired Profile screen displaying the permanent `KMP-XXXX` ID with 1-tap copy morph,
 * editable display name, avatar accent picker, head-to-head friend duel stats, and quick links.
 */
@Composable
fun ProfileScreen(
    identity: UserIdentity,
    friends: List<FriendRecord>,
    progression: ProgressionState,
    windowSizeClass: WindowWidthSizeClass,
    onUpdateDisplayName: (String) -> Unit,
    onCycleAvatarAccent: () -> Unit,
    onOpenCognitiveStats: () -> Unit,
    onOpenVault: () -> Unit,
    onUpdatePrivacyMode: (PrivacyMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (windowSizeClass == WindowWidthSizeClass.Compact) 600.dp else 760.dp
    val clipboardManager = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf(false) }
    var nameDraft by remember(identity.displayName) { mutableStateOf(identity.displayName) }

    LaunchedEffect(copiedId) {
        if (copiedId) {
            delay(1800L)
            copiedId = false
        }
    }

    val accentColor = AvatarAccentPalette[
        identity.avatarAccentIndex.mod(AvatarAccentPalette.size),
    ]
    val totalWins = friends.sumOf { it.duelWins }
    val totalLosses = friends.sumOf { it.duelLosses }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "OFFLINE IDENTITY & HEAD-TO-HEAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }

            // Identity & 1-tap copy card
            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TactileTile(
                            onClick = onCycleAvatarAccent,
                            backgroundColor = accentColor.copy(alpha = 0.18f),
                            restingBorderColor = accentColor.copy(alpha = 0.55f),
                            cornerRadius = 24.dp,
                            modifier = Modifier.size(46.dp),
                        ) {
                            Text(
                                text = identity.displayName.take(2).uppercase(),
                                style = LinearTextStyles.TabularDigit.copy(fontSize = 15.sp),
                                color = accentColor,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PERMANENT USER ID",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = SplitSignalColors.TextSecondary,
                            )
                            Text(
                                text = identity.userId,
                                style = LinearTextStyles.TabularDigit.copy(fontSize = 18.sp),
                                color = SplitSignalColors.TextPrimary,
                            )
                        }
                    }

                    TactileTile(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(identity.userId))
                            copiedId = true
                        },
                        backgroundColor = SplitSignalColors.SurfaceTile,
                        restingBorderColor = if (copiedId) {
                            SplitSignalColors.AccentEmerald.copy(alpha = 0.5f)
                        } else {
                            SplitSignalColors.BorderSubtle
                        },
                        cornerRadius = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AnimatedContent(
                                targetState = copiedId,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "CopyMorphIcon",
                            ) { isCopied ->
                                Icon(
                                    imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = if (isCopied) "Copied" else "Copy User ID",
                                    tint = if (isCopied) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = if (copiedId) "Copied" else "Copy",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (copiedId) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = SplitSignalColors.BorderSubtle)

                // Editable display name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DISPLAY NAME",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SplitSignalColors.TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SplitSignalColors.SurfaceTile,
                                unfocusedContainerColor = SplitSignalColors.SurfaceTile,
                                focusedBorderColor = SplitSignalColors.BorderFocus,
                                unfocusedBorderColor = SplitSignalColors.BorderSubtle,
                                focusedTextColor = SplitSignalColors.TextPrimary,
                                unfocusedTextColor = SplitSignalColors.TextPrimary,
                                cursorColor = SplitSignalColors.AccentIndigo,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        )

                        val canSave = nameDraft.trim().isNotEmpty() && nameDraft.trim() != identity.displayName
                        Button(
                            onClick = { onUpdateDisplayName(nameDraft.trim()) },
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SplitSignalColors.AccentIndigo,
                                contentColor = SplitSignalColors.TextPrimary,
                                disabledContainerColor = SplitSignalColors.SurfaceTile,
                                disabledContentColor = SplitSignalColors.TextMuted,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                // Avatar accent picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "AVATAR ACCENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SplitSignalColors.TextSecondary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarAccentPalette.forEachIndexed { index, color ->
                            val isSelected = identity.avatarAccentIndex.mod(AvatarAccentPalette.size) == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 18.dp else 14.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SplitSignalColors.TextPrimary else SplitSignalColors.BorderSubtle,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                        MatiksChipButton(
                            label = "Cycle",
                            accentColor = accentColor,
                            onClick = onCycleAvatarAccent,
                        )
                    }
                }
            }

            // Nearby Visibility (Public / Friends / Private)
            NearbyVisibilityCard(
                currentMode = identity.privacyMode,
                onSelectMode = onUpdatePrivacyMode,
            )

            // Quick links to Cognitive Stats & Vault
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TactileTile(
                    onClick = onOpenCognitiveStats,
                    backgroundColor = SplitSignalColors.SurfaceCard,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Cognitive Stats",
                            tint = SplitSignalColors.AccentIndigo,
                            modifier = Modifier.size(16.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Cognitive Stats",
                                style = MaterialTheme.typography.titleSmall,
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = "Elo ${progression.elo}",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                color = SplitSignalColors.TextSecondary,
                            )
                        }
                    }
                }

                TactileTile(
                    onClick = onOpenVault,
                    backgroundColor = SplitSignalColors.SurfaceCard,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = "Vault",
                            tint = SplitSignalColors.AccentEmerald,
                            modifier = Modifier.size(16.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Vault",
                                style = MaterialTheme.typography.titleSmall,
                                color = SplitSignalColors.TextPrimary,
                            )
                            Text(
                                text = "${progression.coins} Coins",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                color = SplitSignalColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            // Competence Proofs (Trophies)
            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "COMPETENCE PROOFS (TROPHIES)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                    Text(
                        text = "${progression.unlockedTrophies.size}/${CompetenceTrophy.entries.size}",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                        color = if (progression.unlockedTrophies.isNotEmpty()) {
                            SplitSignalColors.AccentEmerald
                        } else {
                            SplitSignalColors.TextMuted
                        },
                    )
                }

                if (progression.unlockedTrophies.isEmpty()) {
                    Text(
                        text = "No trophies unlocked yet. Earn competence proofs through flawless solves, sub-30s clears, LAN duel wins, and 7-day streaks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompetenceTrophy.entries.forEach { trophy ->
                        val unlocked = trophy in progression.unlockedTrophies
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (unlocked) {
                                        SplitSignalColors.AccentEmerald.copy(alpha = 0.12f)
                                    } else {
                                        SplitSignalColors.SurfaceTile
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (unlocked) {
                                        SplitSignalColors.AccentEmerald.copy(alpha = 0.45f)
                                    } else {
                                        SplitSignalColors.BorderSubtle
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = trophy.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (unlocked) SplitSignalColors.TextPrimary else SplitSignalColors.TextSecondary,
                                )
                                Text(
                                    text = trophy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unlocked) SplitSignalColors.TextSecondary else SplitSignalColors.TextMuted,
                                )
                            }
                            Text(
                                text = if (unlocked) "UNLOCKED" else "LOCKED",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp),
                                color = if (unlocked) SplitSignalColors.AccentEmerald else SplitSignalColors.TextMuted,
                            )
                        }
                    }
                }
            }

            // Head-to-head duel stats across saved friends
            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HEAD-TO-HEAD DUEL STATS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                    Text(
                        text = "${totalWins}W–${totalLosses}L",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                        color = SplitSignalColors.AccentEmerald,
                    )
                }

                if (friends.isEmpty()) {
                    Text(
                        text = "No saved friends yet. Add peers from Nearby to track head-to-head duel records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        friends.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SplitSignalColors.SurfaceTile)
                                    .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SplitSignalColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = friend.userId,
                                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                                        color = SplitSignalColors.TextSecondary,
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "${friend.duelWins}W",
                                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                                        color = SplitSignalColors.AccentEmerald,
                                    )
                                    Text(
                                        text = "${friend.duelLosses}L",
                                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                                        color = SplitSignalColors.TextSecondary,
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


@Composable
private fun NearbyVisibilityCard(
    currentMode: PrivacyMode,
    onSelectMode: (PrivacyMode) -> Unit,
) {
    val badgeColor = when (currentMode) {
        PrivacyMode.PUBLIC -> SplitSignalColors.AccentEmerald
        PrivacyMode.FRIENDS_ONLY -> SplitSignalColors.AccentIndigo
        PrivacyMode.PRIVATE -> SplitSignalColors.TextSecondary
    }
    val description = when (currentMode) {
        PrivacyMode.PUBLIC -> "Visible to all players on your local Wi-Fi network."
        PrivacyMode.FRIENDS_ONLY -> "Only saved friends can see your device on Nearby."
        PrivacyMode.PRIVATE -> "Hidden from Nearby radar. You can still message friends."
    }

    LinearCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "VISIBILITY",
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = currentMode.label.uppercase(),
                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                color = badgeColor,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SplitSignalColors.SurfaceTile)
                .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PrivacyMode.entries.forEach { mode ->
                val selected = mode == currentMode
                val bgColor = if (selected) SplitSignalColors.AccentIndigo else Color.Transparent
                val textColor = if (selected) SplitSignalColors.TextPrimary else SplitSignalColors.TextSecondary
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(bgColor)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(role = Role.Tab) { onSelectMode(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = SplitSignalColors.TextSecondary,
        )
    }
}
