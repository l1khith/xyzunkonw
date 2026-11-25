package com.splitsignal.app.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.splitsignal.core.domain.SyncPacket

/**
 * Linear-inspired Friends screen displaying incoming friend requests, online friends in LAN range,
 * and offline saved friends with clickable chat history access.
 */
@Composable
fun FriendsScreen(
    nearbyState: NearbyDevicesState,
    pendingRequests: List<SyncPacket.FriendRequest>,
    searchQuery: String,
    showSearchBar: Boolean,
    windowSizeClass: WindowWidthSizeClass,
    onSearchQueryChange: (String) -> Unit,
    onAcceptRequest: (SyncPacket.FriendRequest) -> Unit,
    onDeclineRequest: (SyncPacket.FriendRequest) -> Unit,
    onDuelPeer: (DiscoveredPeer) -> Unit,
    onChatPeer: (DiscoveredPeer) -> Unit,
    onSendFileToPeer: (DiscoveredPeer) -> Unit,
    onViewOfflineFriend: (FriendRecord) -> Unit,
    onRemoveFriend: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (windowSizeClass == WindowWidthSizeClass.Compact) 600.dp else 760.dp
    var friendPendingRemoval by remember { mutableStateOf<Pair<String, String>?>(null) }
    val trimmedQuery = searchQuery.trim()
    val filteredOnline = if (trimmedQuery.isEmpty()) {
        nearbyState.friendsInRange
    } else {
        nearbyState.friendsInRange.filter { peer ->
            peer.displayName.contains(trimmedQuery, ignoreCase = true) ||
                peer.userId.contains(trimmedQuery, ignoreCase = true)
        }
    }
    val filteredOffline = if (trimmedQuery.isEmpty()) {
        nearbyState.savedFriendsOffline
    } else {
        nearbyState.savedFriendsOffline.filter { friend ->
            friend.displayName.contains(trimmedQuery, ignoreCase = true) ||
                friend.userId.contains(trimmedQuery, ignoreCase = true)
        }
    }

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
                        text = "Friends",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SplitSignalColors.TextPrimary,
                    )
                    Text(
                        text = "LOCAL ROSTER & REQUESTS",
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
                                if (nearbyState.friendsInRange.isNotEmpty()) {
                                    SplitSignalColors.AccentEmerald
                                } else {
                                    SplitSignalColors.TextMuted
                                },
                            ),
                    )
                    Text(
                        text = "${nearbyState.friendsInRange.size} ONLINE",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = if (nearbyState.friendsInRange.isNotEmpty()) {
                            SplitSignalColors.AccentEmerald
                        } else {
                            SplitSignalColors.TextSecondary
                        },
                    )
                }
            }

            if (showSearchBar) {
                FriendsSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            if (pendingRequests.isNotEmpty()) {
                FriendsSection(title = "PENDING REQUESTS (${pendingRequests.size})") {
                    pendingRequests.forEach { request ->
                        PendingRequestRow(
                            request = request,
                            onAccept = { onAcceptRequest(request) },
                            onDecline = { onDeclineRequest(request) },
                        )
                    }
                }
            }

            if (filteredOnline.isNotEmpty()) {
                FriendsSection(title = "ONLINE NOW (${filteredOnline.size})") {
                    filteredOnline.forEach { peer ->
                        OnlineFriendRow(
                            peer = peer,
                            onDuel = { onDuelPeer(peer) },
                            onChat = { onChatPeer(peer) },
                            onSendFile = { onSendFileToPeer(peer) },
                            onRemove = { friendPendingRemoval = peer.userId to peer.displayName },
                        )
                    }
                }
            }

            if (filteredOffline.isNotEmpty()) {
                FriendsSection(title = "OFFLINE") {
                    filteredOffline.forEach { friend ->
                        OfflineFriendHistoryRow(
                            friend = friend,
                            onClick = { onViewOfflineFriend(friend) },
                            onRemove = { friendPendingRemoval = friend.userId to friend.displayName },
                        )
                    }
                }
            }

            if (pendingRequests.isEmpty() && filteredOnline.isEmpty() && filteredOffline.isEmpty()) {
                FriendsEmptyStateCard(hasFilter = trimmedQuery.isNotEmpty())
            }
        }

        val removalTarget = friendPendingRemoval
        if (removalTarget != null) {
            RemoveFriendConfirmModal(
                friendName = removalTarget.second,
                onConfirm = {
                    onRemoveFriend(removalTarget.first)
                    friendPendingRemoval = null
                },
                onDismiss = { friendPendingRemoval = null },
            )
        }
    }
}

@Composable
private fun FriendsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.SurfaceCard)
            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search friends",
            tint = SplitSignalColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Filter by name or KMP-XXXX...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextMuted,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = SplitSignalColors.TextPrimary,
                ),
                cursorBrush = SolidColor(SplitSignalColors.AccentIndigo),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(SplitSignalColors.SurfaceTile)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(role = Role.Button) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = SplitSignalColors.TextSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun FriendsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
            color = SplitSignalColors.TextSecondary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun PendingRequestRow(
    request: SyncPacket.FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = request.fromName,
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = request.fromUserId,
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 12.sp),
                    color = SplitSignalColors.TextSecondary,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TactileTile(
                    onClick = onDecline,
                    backgroundColor = SplitSignalColors.SurfaceTile,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "Decline",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
                TactileTile(
                    onClick = onAccept,
                    backgroundColor = SplitSignalColors.AccentIndigo,
                    restingBorderColor = SplitSignalColors.AccentIndigo,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "Accept",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineFriendRow(
    peer: DiscoveredPeer,
    onDuel: () -> Unit,
    onChat: () -> Unit,
    onSendFile: () -> Unit,
    onRemove: () -> Unit,
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
                        .background(SplitSignalColors.AccentEmerald),
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
                    Text(
                        text = "ELO: ${peer.elo} | Streak: ${peer.streakDays}d",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = SplitSignalColors.AccentIndigo,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FriendActionIconButton(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = "Chat with ${peer.displayName}",
                    tint = SplitSignalColors.TextPrimary,
                    onClick = onChat,
                )
                FriendActionIconButton(
                    icon = Icons.Rounded.SportsEsports,
                    contentDescription = "Duel ${peer.displayName}",
                    tint = SplitSignalColors.AccentIndigo,
                    onClick = onDuel,
                )
                FriendActionIconButton(
                    icon = Icons.Rounded.AttachFile,
                    contentDescription = "Send file to ${peer.displayName}",
                    tint = SplitSignalColors.TextPrimary,
                    onClick = onSendFile,
                )
                FriendActionIconButton(
                    icon = Icons.Rounded.PersonRemove,
                    contentDescription = "Remove ${peer.displayName}",
                    tint = SplitSignalColors.TextSecondary,
                    onClick = onRemove,
                )
            }
        }
    }
}

@Composable
private fun OfflineFriendHistoryRow(
    friend: FriendRecord,
    onClick: () -> Unit,
    onRemove: () -> Unit,
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
                FriendActionIconButton(
                    icon = Icons.Rounded.PersonRemove,
                    contentDescription = "Remove ${friend.displayName}",
                    tint = SplitSignalColors.TextSecondary,
                    onClick = onRemove,
                )
            }
        }
    }
}

@Composable
private fun FriendsEmptyStateCard(hasFilter: Boolean) {
    LinearCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    imageVector = Icons.Rounded.Group,
                    contentDescription = "No friends",
                    tint = SplitSignalColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (hasFilter) "No matching friends" else "No friends added yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = if (hasFilter) {
                        "Try clearing your search filter."
                    } else {
                        "Discover players on the Nearby radar or add a friend by KMP-XXXX ID."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun FriendActionIconButton(
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
private fun RemoveFriendConfirmModal(
    friendName: String,
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
                        imageVector = Icons.Rounded.PersonRemove,
                        contentDescription = "Remove Friend",
                        tint = SplitSignalColors.AccentCrimson,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "REMOVE FRIEND",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.AccentCrimson,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Remove $friendName?",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "This removes them from your local friends list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }

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
                        backgroundColor = SplitSignalColors.AccentCrimson,
                        restingBorderColor = SplitSignalColors.AccentCrimson,
                        cornerRadius = 8.dp,
                    ) {
                        Text(
                            text = "Remove",
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
