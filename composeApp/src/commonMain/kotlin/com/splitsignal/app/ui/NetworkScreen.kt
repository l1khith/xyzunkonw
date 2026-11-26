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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.graphics.SolidColor
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
import com.splitsignal.core.domain.SyncPacket

/**
 * Unified Network screen combining manual local Wi-Fi scanning, inline peer search,
 * incoming friend requests, friends in range, discovered LAN peers, and offline friends.
 */
@Composable
fun NetworkScreen(
    nearbyState: NearbyDevicesState,
    pendingRequests: List<SyncPacket.FriendRequest>,
    isScanning: Boolean,
    searchQuery: String,
    windowSizeClass: WindowWidthSizeClass,
    onToggleScan: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAcceptRequest: (SyncPacket.FriendRequest) -> Unit,
    onDeclineRequest: (SyncPacket.FriendRequest) -> Unit,
    onDuelPeer: (DiscoveredPeer) -> Unit,
    onChatPeer: (DiscoveredPeer) -> Unit,
    onSendFileToPeer: (DiscoveredPeer) -> Unit,
    onAddFriend: (DiscoveredPeer) -> Unit,
    onViewOfflineFriend: (FriendRecord) -> Unit,
    onRemoveFriend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (windowSizeClass == WindowWidthSizeClass.Compact) 600.dp else 760.dp
    var pendingAddFriendPeer by remember { mutableStateOf<DiscoveredPeer?>(null) }
    var friendPendingRemoval by remember { mutableStateOf<Pair<String, String>?>(null) }

    val trimmedQuery = searchQuery.trim()
    val filteredRequests = remember(pendingRequests, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            pendingRequests
        } else {
            pendingRequests.filter { req ->
                req.fromName.contains(trimmedQuery, ignoreCase = true) ||
                    req.fromUserId.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val filteredFriendsInRange = remember(nearbyState.friendsInRange, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            nearbyState.friendsInRange
        } else {
            nearbyState.friendsInRange.filter { peer ->
                peer.displayName.contains(trimmedQuery, ignoreCase = true) ||
                    peer.userId.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val filteredUnknownNearby = remember(nearbyState.unknownNearby, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            nearbyState.unknownNearby
        } else {
            nearbyState.unknownNearby.filter { peer ->
                peer.displayName.contains(trimmedQuery, ignoreCase = true) ||
                    peer.userId.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val filteredOfflineFriends = remember(nearbyState.savedFriendsOffline, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            nearbyState.savedFriendsOffline
        } else {
            nearbyState.savedFriendsOffline.filter { friend ->
                friend.displayName.contains(trimmedQuery, ignoreCase = true) ||
                    friend.userId.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val onlineCount = nearbyState.friendsInRange.size + nearbyState.unknownNearby.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                NetworkHeader(onlineCount = onlineCount)
            }

            item(key = "radar_card") {
                NetworkScanCard(
                    isScanning = isScanning,
                    nearbyCount = onlineCount,
                    onToggleScan = onToggleScan,
                )
            }

            item(key = "search_bar") {
                NetworkSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            if (filteredRequests.isNotEmpty()) {
                item(key = "section_pending") {
                    NetworkSectionHeader(title = "PENDING REQUESTS (${filteredRequests.size})")
                }
                items(
                    items = filteredRequests,
                    key = { "req_${it.fromUserId}" },
                ) { request ->
                    NetworkPendingRequestRow(
                        request = request,
                        onAccept = { onAcceptRequest(request) },
                        onDecline = { onDeclineRequest(request) },
                    )
                }
            }

            if (filteredFriendsInRange.isNotEmpty()) {
                item(key = "section_friends_in_range") {
                    NetworkSectionHeader(title = "FRIENDS IN RANGE (${filteredFriendsInRange.size})")
                }
                items(
                    items = filteredFriendsInRange,
                    key = { "friend_online_${it.userId}" },
                ) { peer ->
                    NetworkFriendInRangeRow(
                        peer = peer,
                        onChat = { onChatPeer(peer) },
                        onDuel = { onDuelPeer(peer) },
                        onSendFile = { onSendFileToPeer(peer) },
                        onRemove = { friendPendingRemoval = peer.userId to peer.displayName },
                    )
                }
            }

            if (filteredUnknownNearby.isNotEmpty()) {
                item(key = "section_discovered") {
                    NetworkSectionHeader(title = "DISCOVERED ON LOCAL WI-FI (${filteredUnknownNearby.size})")
                }
                items(
                    items = filteredUnknownNearby,
                    key = { "discovered_${it.userId}" },
                ) { peer ->
                    NetworkDiscoveredPeerRow(
                        peer = peer,
                        onAddFriend = { pendingAddFriendPeer = peer },
                        onChat = { onChatPeer(peer) },
                        onSendFile = { onSendFileToPeer(peer) },
                    )
                }
            }

            if (filteredOfflineFriends.isNotEmpty()) {
                item(key = "section_offline") {
                    NetworkSectionHeader(title = "OFFLINE FRIENDS (${filteredOfflineFriends.size})")
                }
                items(
                    items = filteredOfflineFriends,
                    key = { "friend_offline_${it.userId}" },
                ) { friend ->
                    NetworkOfflineFriendRow(
                        friend = friend,
                        onClick = { onViewOfflineFriend(friend) },
                        onRemove = { friendPendingRemoval = friend.userId to friend.displayName },
                    )
                }
            }

            if (
                filteredRequests.isEmpty() &&
                filteredFriendsInRange.isEmpty() &&
                filteredUnknownNearby.isEmpty() &&
                filteredOfflineFriends.isEmpty()
            ) {
                item(key = "empty_state") {
                    NetworkEmptyStateCard(
                        isScanning = isScanning,
                        hasFilter = trimmedQuery.isNotEmpty(),
                    )
                }
            }
        }

        val addTarget = pendingAddFriendPeer
        if (addTarget != null) {
            NetworkAddFriendConfirmModal(
                peer = addTarget,
                onConfirm = {
                    onAddFriend(addTarget)
                    pendingAddFriendPeer = null
                },
                onDismiss = { pendingAddFriendPeer = null },
            )
        }

        val removalTarget = friendPendingRemoval
        if (removalTarget != null) {
            NetworkRemoveFriendConfirmModal(
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
private fun NetworkHeader(onlineCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Network",
                style = MaterialTheme.typography.headlineSmall,
                color = SplitSignalColors.TextPrimary,
            )
            Text(
                text = "LOCAL WI-FI & FRIENDS",
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
                        if (onlineCount > 0) SplitSignalColors.AccentEmerald else SplitSignalColors.TextMuted,
                    ),
            )
            Text(
                text = "$onlineCount ONLINE",
                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                color = if (onlineCount > 0) SplitSignalColors.AccentEmerald else SplitSignalColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun NetworkScanCard(
    isScanning: Boolean,
    nearbyCount: Int,
    onToggleScan: () -> Unit,
) {
    LinearCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isScanning) {
                    PulsingRadarIndicator()
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SplitSignalColors.SurfaceTile)
                            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = "Local Wi-Fi Radar Off",
                            tint = SplitSignalColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (isScanning) "Scanning local Wi-Fi..." else "Local Wi-Fi Radar Off",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isScanning) SplitSignalColors.TextPrimary else SplitSignalColors.TextSecondary,
                    )
                    Text(
                        text = if (!isScanning) {
                            "Manual scan keeps radio & battery quiet until you connect."
                        } else if (nearbyCount == 0) {
                            "Broadcasting presence on your local network..."
                        } else {
                            "$nearbyCount peer${if (nearbyCount == 1) "" else "s"} visible on local Wi-Fi."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SplitSignalColors.TextMuted,
                    )
                }
            }

            if (isScanning) {
                TactileTile(
                    onClick = onToggleScan,
                    backgroundColor = SplitSignalColors.SurfaceTile,
                    restingBorderColor = SplitSignalColors.BorderFocus,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "Stop Scanning",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            } else {
                TactileTile(
                    onClick = onToggleScan,
                    backgroundColor = SplitSignalColors.AccentIndigo,
                    restingBorderColor = SplitSignalColors.AccentIndigo,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "Start Scanning",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingRadarIndicator() {
    val transition = rememberInfiniteTransition(label = "NetworkRadar")
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

    Canvas(modifier = Modifier.size(44.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f - 2.dp.toPx()
        val radarColor = SplitSignalColors.AccentIndigo

        drawCircle(
            color = radarColor.copy(alpha = 0.16f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = radarColor.copy(alpha = (1f - primaryPulse) * 0.55f),
            radius = maxRadius * primaryPulse.coerceAtLeast(0.15f),
            center = center,
            style = Stroke(width = 1.75.dp.toPx()),
        )
        drawCircle(
            color = radarColor.copy(alpha = (1f - secondaryPulse) * 0.55f),
            radius = maxRadius * secondaryPulse.coerceAtLeast(0.15f),
            center = center,
            style = Stroke(width = 1.75.dp.toPx()),
        )
        drawCircle(
            color = radarColor.copy(alpha = (1f - primaryPulse) * 0.14f),
            radius = maxRadius * primaryPulse,
            center = center,
        )
        drawCircle(
            color = radarColor,
            radius = 4.dp.toPx(),
            center = center,
        )
    }
}

@Composable
private fun NetworkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.BgCanvas)
            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search users",
            tint = SplitSignalColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search users or KMP-XXXX...",
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
private fun NetworkSectionHeader(title: String) {
    Text(
        text = title,
        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
        color = SplitSignalColors.TextSecondary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun NetworkPendingRequestRow(
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
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkFriendInRangeRow(
    peer: DiscoveredPeer,
    onChat: () -> Unit,
    onDuel: () -> Unit,
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
                        .background(Color(0xFFF7F8F8)), // Monochrome online dot
                )
                Text(
                    text = "${peer.displayName} · ${peer.userId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TactileTile(
                onClick = onChat,
                backgroundColor = SplitSignalColors.AccentIndigo,
                restingBorderColor = SplitSignalColors.AccentIndigo,
                cornerRadius = 8.dp,
            ) {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun NetworkDiscoveredPeerRow(
    peer: DiscoveredPeer,
    onAddFriend: () -> Unit,
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
                        .border(1.5.dp, SplitSignalColors.TextSecondary, CircleShape),
                )
                Text(
                    text = "${peer.displayName} · ${peer.userId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TactileTile(
                    onClick = onAddFriend,
                    backgroundColor = SplitSignalColors.SurfaceTile,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "+Add",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
                TactileTile(
                    onClick = onChat,
                    backgroundColor = SplitSignalColors.AccentIndigo,
                    restingBorderColor = SplitSignalColors.AccentIndigo,
                    cornerRadius = 8.dp,
                ) {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkOfflineFriendRow(
    friend: FriendRecord,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    LinearCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick),
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${friend.displayName} · ${friend.userId}",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Last: 2d",
                style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                color = SplitSignalColors.TextMuted,
            )
        }
    }
}

@Composable
private fun NetworkActionIconButton(
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
        modifier = Modifier.size(34.dp),
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
private fun NetworkEmptyStateCard(
    isScanning: Boolean,
    hasFilter: Boolean,
) {
    LinearCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SplitSignalColors.SurfaceTile)
                    .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = SplitSignalColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = when {
                        hasFilter -> "No matching peers or friends"
                        isScanning -> "Listening on local Wi-Fi..."
                        else -> "No peers or saved friends yet"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = when {
                        hasFilter -> "Try another display name or KMP-XXXX tag."
                        isScanning -> "Nearby SplitSignal devices on your Wi-Fi will appear here."
                        else -> "Tap Start Scanning above to discover nearby players."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun NetworkAddFriendConfirmModal(
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

@Composable
private fun NetworkRemoveFriendConfirmModal(
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
