package com.splitsignal.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.DiscoveredPeer
import com.splitsignal.core.domain.FriendRecord
import com.splitsignal.core.domain.NearbyDevicesState
import com.splitsignal.core.domain.PeerStatus

/**
 * Linear-inspired Nearby radar screen with an animated Canvas Wi-Fi pulse scanner
 * and discovered LAN peers (`unknownNearby` + `friendsInRange`) with quick actions.
 */
@Composable
fun NearbyScreen(
    nearbyState: NearbyDevicesState,
    windowSizeClass: WindowWidthSizeClass,
    onDuelPeer: (DiscoveredPeer) -> Unit,
    onChatPeer: (DiscoveredPeer) -> Unit,
    onSendFileToPeer: (DiscoveredPeer) -> Unit,
    onAddFriend: (DiscoveredPeer) -> Unit,
    onViewOfflineFriend: (FriendRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (windowSizeClass == WindowWidthSizeClass.Compact) 600.dp else 760.dp
    val discoveredDevices = nearbyState.unknownNearby + nearbyState.friendsInRange
    val nearbyCount = discoveredDevices.size
    var pendingAddFriendPeer by remember { mutableStateOf<DiscoveredPeer?>(null) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Nearby",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "LOCAL LAN RADAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SplitSignalColors.SurfaceCard)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (nearbyCount > 0) SplitSignalColors.AccentEmerald else SplitSignalColors.TextMuted,
                            ),
                    )
                    Text(
                        text = "$nearbyCount ONLINE",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = if (nearbyCount > 0) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
                    )
                }
            }

            NearbyRadarScannerCard(nearbyCount = nearbyCount)

            if (discoveredDevices.isNotEmpty()) {
                NearbySection(
                    title = "DISCOVERED DEVICES",
                    count = discoveredDevices.size,
                ) {
                    nearbyState.unknownNearby.forEach { peer ->
                        DiscoveredDeviceRow(
                            peer = peer,
                            isFriend = false,
                            onAddFriend = { pendingAddFriendPeer = peer },
                            onDuel = { onDuelPeer(peer) },
                            onChat = { onChatPeer(peer) },
                            onSendFile = { onSendFileToPeer(peer) },
                        )
                    }
                    nearbyState.friendsInRange.forEach { peer ->
                        DiscoveredDeviceRow(
                            peer = peer,
                            isFriend = true,
                            onAddFriend = { onAddFriend(peer) },
                            onDuel = { onDuelPeer(peer) },
                            onChat = { onChatPeer(peer) },
                            onSendFile = { onSendFileToPeer(peer) },
                        )
                    }
                }
            }

            if (nearbyState.savedFriendsOffline.isNotEmpty()) {
                NearbySection(
                    title = "OFFLINE FRIENDS",
                    count = nearbyState.savedFriendsOffline.size,
                ) {
                    nearbyState.savedFriendsOffline.forEach { friend ->
                        OfflineFriendRow(
                            friend = friend,
                            onClick = { onViewOfflineFriend(friend) },
                        )
                    }
                }
            }
        }

        val targetPeer = pendingAddFriendPeer
        if (targetPeer != null) {
            AddFriendConfirmModal(
                peer = targetPeer,
                onConfirm = {
                    onAddFriend(targetPeer)
                    pendingAddFriendPeer = null
                },
                onDismiss = { pendingAddFriendPeer = null },
            )
        }
    }
}

@Composable
private fun NearbyRadarScannerCard(nearbyCount: Int) {
    val transition = rememberInfiniteTransition(label = "NearbyRadar")
    val primaryPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "PrimaryRadarPulse",
    )
    val secondaryPulse = (primaryPulse + 0.5f) % 1f

    LinearCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f - 2.dp.toPx()
                val radarColor = SplitSignalColors.AccentIndigo

                // Static guide rings
                drawCircle(
                    color = radarColor.copy(alpha = 0.16f),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = radarColor.copy(alpha = 0.22f),
                    radius = maxRadius * 0.55f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )

                // Pulsing wave 1
                drawCircle(
                    color = radarColor.copy(alpha = (1f - primaryPulse) * 0.55f),
                    radius = maxRadius * primaryPulse.coerceAtLeast(0.12f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Pulsing wave 2
                drawCircle(
                    color = radarColor.copy(alpha = (1f - secondaryPulse) * 0.55f),
                    radius = maxRadius * secondaryPulse.coerceAtLeast(0.12f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Subtle fill pulse
                drawCircle(
                    color = radarColor.copy(alpha = (1f - primaryPulse) * 0.14f),
                    radius = maxRadius * primaryPulse,
                    center = center,
                )

                // Center emitter dot
                drawCircle(
                    color = radarColor,
                    radius = 5.dp.toPx(),
                    center = center,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scanning local Wi-Fi...",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextSecondary,
                )
                Text(
                    text = if (nearbyCount == 0) {
                        "Your status is not in the cloud. It's in the room."
                    } else {
                        "$nearbyCount device${if (nearbyCount == 1) "" else "s"} broadcasting on your local network."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun NearbySection(
    title: String,
    count: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = SplitSignalColors.TextSecondary,
            )
            Text(
                text = "$count",
                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                color = SplitSignalColors.TextMuted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    peer: DiscoveredPeer,
    isFriend: Boolean,
    onAddFriend: () -> Unit,
    onDuel: () -> Unit,
    onChat: () -> Unit,
    onSendFile: () -> Unit,
) {
    LinearCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 14.dp,
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
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFriend) SplitSignalColors.AccentEmerald else SplitSignalColors.AccentIndigo,
                        ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = peer.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SplitSignalColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (peer.status == PeerStatus.IN_GAME) {
                            Text(
                                text = "IN GAME",
                                style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp),
                                color = SplitSignalColors.AccentAmber,
                            )
                        }
                    }
                    Text(
                        text = peer.userId,
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                        color = SplitSignalColors.TextSecondary,
                    )
                    if (isFriend) {
                        Text(
                            text = "ELO: ${peer.elo} | Streak: ${peer.streakDays}d",
                            style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                            color = SplitSignalColors.AccentIndigo,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isFriend) {
                    TactileTile(
                        onClick = onAddFriend,
                        backgroundColor = SplitSignalColors.SurfaceTile,
                        restingBorderColor = SplitSignalColors.BorderSubtle,
                        cornerRadius = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Add Friend",
                                tint = SplitSignalColors.AccentIndigo,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "+ Add Friend",
                                style = MaterialTheme.typography.labelMedium,
                                color = SplitSignalColors.TextPrimary,
                            )
                        }
                    }
                } else {
                    CompactIconActionButton(
                        icon = Icons.Rounded.SportsEsports,
                        contentDescription = "Duel ${peer.displayName}",
                        tint = SplitSignalColors.AccentIndigo,
                        onClick = onDuel,
                    )
                }

                CompactTextActionButton(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    label = "Chat",
                    onClick = onChat,
                )
                CompactTextActionButton(
                    icon = Icons.Rounded.AttachFile,
                    label = "File",
                    onClick = onSendFile,
                )
            }
        }
    }
}

@Composable
private fun OfflineFriendRow(
    friend: FriendRecord,
    onClick: () -> Unit,
) {
    LinearCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick),
        contentPadding = 14.dp,
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
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SplitSignalColors.TextMuted),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = SplitSignalColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = friend.userId,
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = SplitSignalColors.TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = SplitSignalColors.TextMuted,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(SplitSignalColors.SurfaceTile)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "${friend.duelWins}W–${friend.duelLosses}L",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    TactileTile(
        onClick = onClick,
        backgroundColor = SplitSignalColors.SurfaceTile,
        restingBorderColor = SplitSignalColors.BorderSubtle,
        cornerRadius = 8.dp,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CompactTextActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TactileTile(
        onClick = onClick,
        backgroundColor = SplitSignalColors.SurfaceTile,
        restingBorderColor = SplitSignalColors.BorderSubtle,
        cornerRadius = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SplitSignalColors.TextPrimary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SplitSignalColors.TextPrimary,
            )
        }
    }
}


@Composable
private fun AddFriendConfirmModal(
    peer: DiscoveredPeer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas.copy(alpha = 0.88f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
            borderColor = SplitSignalColors.BorderSubtle,
            contentPadding = 20.dp,
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
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = "Add Friend",
                        tint = SplitSignalColors.AccentIndigo,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "SEND FRIEND REQUEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.AccentIndigo,
                )
            }

            Text(
                text = "Send friend request to ${peer.displayName} (${peer.userId})?",
                style = MaterialTheme.typography.bodyMedium,
                color = SplitSignalColors.TextPrimary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TactileTile(
                        onClick = onDismiss,
                        backgroundColor = SplitSignalColors.SurfaceTile,
                        restingBorderColor = SplitSignalColors.BorderSubtle,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            color = SplitSignalColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                    TactileTile(
                        onClick = onConfirm,
                        backgroundColor = SplitSignalColors.AccentIndigo,
                        restingBorderColor = SplitSignalColors.AccentIndigo,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Send Request",
                            style = MaterialTheme.typography.labelMedium,
                            color = SplitSignalColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}
