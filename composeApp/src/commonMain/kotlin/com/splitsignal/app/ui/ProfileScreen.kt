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
import com.splitsignal.core.domain.FileTransferItem
import com.splitsignal.core.domain.FriendRecord
import com.splitsignal.core.domain.PrivacyMode
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.TransferStatus
import com.splitsignal.core.domain.UserIdentity
import com.splitsignal.core.domain.formatByteSize
import kotlinx.coroutines.delay

private val AvatarAccentPalette: List<Color> = listOf(
    SplitSignalColors.AccentIndigo,
    SplitSignalColors.AccentIndigo,
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
    onUpdatePrivacyMode: (PrivacyMode) -> Unit = {},
    transfers: List<FileTransferItem> = emptyList(),
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
            var isEditingName by remember { mutableStateOf(false) }

            // User Identity Card: Name [Edit] + ID: KMP-XXXX [Copy]
            LinearCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = identity.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = SplitSignalColors.TextPrimary,
                        )
                        TactileTile(
                            onClick = { isEditingName = !isEditingName },
                            backgroundColor = SplitSignalColors.SurfaceTile,
                            restingBorderColor = SplitSignalColors.BorderSubtle,
                            cornerRadius = 8.dp,
                        ) {
                            Text(
                                text = if (isEditingName) "Close" else "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = SplitSignalColors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    if (isEditingName) {
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
                            Button(
                                onClick = {
                                    onUpdateDisplayName(nameDraft.trim())
                                    isEditingName = false
                                },
                                enabled = nameDraft.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplitSignalColors.AccentIndigo,
                                    contentColor = SplitSignalColors.TextPrimary,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Save")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ID: ${identity.userId}",
                            style = LinearTextStyles.TabularDigit.copy(fontSize = 15.sp),
                            color = SplitSignalColors.TextSecondary,
                        )
                        TactileTile(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(identity.userId))
                                copiedId = true
                            },
                            backgroundColor = SplitSignalColors.SurfaceTile,
                            restingBorderColor = if (copiedId) SplitSignalColors.AccentIndigo.copy(alpha = 0.5f) else SplitSignalColors.BorderSubtle,
                            cornerRadius = 8.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (copiedId) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy User ID",
                                    tint = if (copiedId) SplitSignalColors.AccentIndigo else SplitSignalColors.TextPrimary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = if (copiedId) "Copied" else "Copy",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (copiedId) SplitSignalColors.AccentIndigo else SplitSignalColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // Borderless Stat Row (tnum)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${progression.streakDays} Days",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "Streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${progression.elo}",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "Elo",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${progression.solvedCount}",
                        style = LinearTextStyles.TabularDigit.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "Solved",
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }

            // Section: RECENT TRANSFERS & MATCHES
            val recentActivities = remember(transfers, progression) {
                buildList {
                    transfers.filter { it.status == TransferStatus.COMPLETED }.forEach { t ->
                        add(t.fileName to formatByteSize(t.fileSizeBytes))
                    }
                    if (progression.dailyCompletedToday) {
                        add("Daily Workout" to "Completed today")
                    }
                    progression.modeClears.forEach { (gameType, clears) ->
                        if (clears > 0) {
                            add(gameType.displayName to "$clears solved")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RECENT TRANSFERS & MATCHES",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = SplitSignalColors.TextSecondary,
                )
                if (recentActivities.isEmpty()) {
                    LinearCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No recent transfers or matches yet. Solved games and LAN file transfers will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SplitSignalColors.TextSecondary,
                        )
                    }
                } else {
                    recentActivities.take(5).forEach { (title, subtitle) ->
                        LinearCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF26B57B),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "$title · $subtitle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SplitSignalColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // Nearby Visibility (Public / Friends / Private)
            NearbyVisibilityCard(
                currentMode = identity.privacyMode,
                onSelectMode = onUpdatePrivacyMode,
            )

            // Quick link to Cognitive Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TactileTile(
                    onClick = onOpenCognitiveStats,
                    backgroundColor = SplitSignalColors.SurfaceCard,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 12.dp,
                    modifier = Modifier.fillMaxWidth(),
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
                val bgColor = if (selected) Color(0xFF5E6AD2) else Color.Transparent
                val textColor = if (selected) Color.White else SplitSignalColors.TextSecondary
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
