package com.splitsignal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.platform.PlatformMediaBridge
import com.splitsignal.core.domain.ChatMessageRecord
import com.splitsignal.core.domain.ChatSessionUiState
import com.splitsignal.core.domain.FileTransferItem
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.domain.SyncPacket
import com.splitsignal.core.domain.TransferStatus
import com.splitsignal.core.domain.TransportConnectionState
import com.splitsignal.core.domain.formatByteSize

/**
 * Ruthlessly simplified Linear-minimalist chat sheet.
 * Features: text bubbles + file transfer cards. No voice, no stickers, no grids.
 */
@Composable
fun DirectChatSheet(
    sessionState: ChatSessionUiState,
    mediaBridge: PlatformMediaBridge,
    onSendMessage: (String) -> Unit,
    onSendSticker: (stickerCode: String, label: String) -> Unit,
    onSendVoiceNote: (PlatformFileHandle) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onAttachFile: () -> Unit,
    onSendGameInvite: () -> Unit = {},
    onAcceptFriendRequest: (fromUserId: String, fromName: String) -> Unit,
    onDeclineFriendRequest: (fromUserId: String) -> Unit,
    onAcceptTransfer: (transferId: String) -> Unit,
    onDeclineTransfer: (transferId: String) -> Unit,
    onCancelTransfer: (transferId: String) -> Unit,
    onShareFile: (filePath: String, mimeType: String) -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val peer = sessionState.activePeer
    val offlineFriend = sessionState.activeOfflineFriend
    val isOfflineHistory = offlineFriend != null && peer == null
    val peerName = peer?.displayName ?: offlineFriend?.displayName ?: "Direct Chat"
    val peerUserId = peer?.userId ?: offlineFriend?.userId ?: "KMP-0000"
    val canInteract = peer != null
    val totalItemCount = sessionState.messages.size + sessionState.transfers.size

    LaunchedEffect(totalItemCount) {
        if (totalItemCount > 0) listState.animateScrollToItem(totalItemCount - 1)
    }

    val sendDraft = {
        val trimmed = draftMessage.trim()
        if (trimmed.isNotEmpty() && peer != null) {
            onSendMessage(trimmed)
            draftMessage = ""
            onTypingChanged(false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas)
            .imePadding(),
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        ChatTopBar(
            peerName = peerName,
            peerUserId = peerUserId,
            isOfflineHistory = isOfflineHistory,
            connectionState = sessionState.connectionState,
            peerIsTyping = sessionState.peerIsTyping,
            canDuel = canInteract,
            onBack = onClose,
            onSendGameInvite = onSendGameInvite,
        )
        HorizontalDivider(thickness = 1.dp, color = SplitSignalColors.BorderSubtle)

        // ── Inline friend-request banners ─────────────────────────────────────
        sessionState.incomingFriendRequests.forEach { request ->
            FriendRequestBanner(
                request = request,
                onAccept = { onAcceptFriendRequest(request.fromUserId, request.fromName) },
                onDecline = { onDeclineFriendRequest(request.fromUserId) },
            )
        }

        // ── Validation error banner ───────────────────────────────────────────
        val validationError = sessionState.validationError
        if (validationError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SplitSignalColors.AccentCrimson.copy(alpha = 0.14f))
                    .border(1.dp, SplitSignalColors.AccentCrimson.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.AccentCrimson,
                    modifier = Modifier.weight(1f),
                )
                MatiksChipButton(label = "Dismiss", accentColor = SplitSignalColors.AccentCrimson, onClick = onDismissError)
            }
        }

        // ── Message list ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (sessionState.messages.isEmpty() && sessionState.transfers.isEmpty()) {
                item(key = "empty_thread") {
                    ChatEmptyState(
                        peerName = peerName,
                        isOfflineHistory = isOfflineHistory,
                        canInteract = canInteract,
                        onSendGameInvite = onSendGameInvite,
                        onAttachFile = onAttachFile,
                    )
                }
            }
            items(sessionState.messages, key = { "msg_${it.messageId}" }) { message ->
                when {
                    message.isSystemEvent -> SystemEventPill(message)
                    else -> ChatBubble(message)
                }
            }
            items(sessionState.transfers, key = { "xfer_${it.transferId}" }) { transfer ->
                FileTransferCard(
                    transfer = transfer,
                    onAccept = { onAcceptTransfer(transfer.transferId) },
                    onDecline = { onDeclineTransfer(transfer.transferId) },
                    onCancel = { onCancelTransfer(transfer.transferId) },
                    onShareFile = onShareFile,
                )
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        HorizontalDivider(thickness = 1.dp, color = SplitSignalColors.BorderSubtle)
        SimpleInputBar(
            draft = draftMessage,
            enabled = canInteract,
            onDraftChange = { v ->
                draftMessage = v
                onTypingChanged(v.isNotBlank())
            },
            onAttachFile = onAttachFile,
            onSend = sendDraft,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    peerName: String,
    peerUserId: String,
    isOfflineHistory: Boolean,
    connectionState: TransportConnectionState,
    peerIsTyping: Boolean,
    canDuel: Boolean,
    onBack: () -> Unit,
    onSendGameInvite: () -> Unit,
) {
    val (statusColor, statusLabel) = when {
        isOfflineHistory -> SplitSignalColors.TextMuted to "Offline History"
        peerIsTyping -> SplitSignalColors.AccentEmerald to "typing..."
        connectionState == TransportConnectionState.CONNECTED ->
            SplitSignalColors.AccentEmerald to "Connected via Local Wi-Fi"
        connectionState == TransportConnectionState.CONNECTING ->
            SplitSignalColors.AccentAmber to "Connecting..."
        connectionState == TransportConnectionState.ERROR ->
            SplitSignalColors.AccentCrimson to "Connection Error"
        else -> SplitSignalColors.TextMuted to "Disconnected"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitSignalColors.BgCanvas)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TactileTile(
                onClick = onBack,
                backgroundColor = SplitSignalColors.SurfaceCard,
                restingBorderColor = SplitSignalColors.BorderSubtle,
                cornerRadius = 10.dp,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = SplitSignalColors.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = SplitSignalColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = peerUserId,
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                        color = SplitSignalColors.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SplitSignalColors.SurfaceCard)
                            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor),
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = statusColor,
                        maxLines = 1,
                    )
                }
            }
        }

        if (canDuel) {
            TactileTile(
                onClick = onSendGameInvite,
                backgroundColor = SplitSignalColors.SurfaceCard,
                restingBorderColor = SplitSignalColors.BorderSubtle,
                cornerRadius = 10.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Duel",
                        tint = SplitSignalColors.AccentIndigo,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(text = "Duel", style = MaterialTheme.typography.labelMedium, color = SplitSignalColors.TextPrimary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState(
    peerName: String,
    isOfflineHistory: Boolean,
    canInteract: Boolean,
    onSendGameInvite: () -> Unit,
    onAttachFile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NodeLinkIllustration(isOffline = isOfflineHistory, modifier = Modifier.size(width = 168.dp, height = 92.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isOfflineHistory) "No saved messages with $peerName" else "Direct LAN Link Ready",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isOfflineHistory)
                    "Messages exchanged with $peerName on local Wi-Fi will appear here."
                else
                    "End-to-end local Wi-Fi session. Zero cloud servers.",
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }

        if (!isOfflineHistory && canInteract) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionPill(icon = Icons.Rounded.SportsEsports, label = "Send Game Invite", accent = SplitSignalColors.AccentIndigo, onClick = onSendGameInvite)
                ActionPill(icon = Icons.Rounded.AttachFile, label = "Share a File", accent = SplitSignalColors.AccentEmerald, onClick = onAttachFile)
            }
        }
    }
}

@Composable
private fun NodeLinkIllustration(isOffline: Boolean, modifier: Modifier = Modifier) {
    val primary = if (isOffline) SplitSignalColors.TextMuted else SplitSignalColors.AccentIndigo
    val peer = if (isOffline) SplitSignalColors.TextMuted else SplitSignalColors.AccentEmerald
    val link = if (isOffline) SplitSignalColors.BorderSubtle else SplitSignalColors.AccentIndigo.copy(alpha = 0.55f)
    Canvas(modifier = modifier) {
        val l = Offset(size.width * 0.22f, size.height * 0.54f)
        val t = Offset(size.width * 0.50f, size.height * 0.24f)
        val r = Offset(size.width * 0.78f, size.height * 0.54f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        drawLine(link, l, r, 1.6.dp.toPx(), StrokeCap.Round, pathEffect = dash)
        drawLine(SplitSignalColors.BorderFocus, l, t, 1.2.dp.toPx(), StrokeCap.Round)
        drawLine(SplitSignalColors.BorderFocus, t, r, 1.2.dp.toPx(), StrokeCap.Round)
        drawCircle(primary.copy(alpha = 0.14f), 20.dp.toPx(), l)
        drawCircle(primary.copy(alpha = 0.35f), 20.dp.toPx(), l, style = Stroke(1.dp.toPx()))
        drawCircle(peer.copy(alpha = 0.14f), 20.dp.toPx(), r)
        drawCircle(peer.copy(alpha = 0.35f), 20.dp.toPx(), r, style = Stroke(1.dp.toPx()))
        drawCircle(SplitSignalColors.SurfaceCard, 10.dp.toPx(), t)
        drawCircle(SplitSignalColors.BorderFocus, 10.dp.toPx(), t, style = Stroke(1.dp.toPx()))
        drawCircle(SplitSignalColors.TextSecondary, 3.dp.toPx(), t)
        drawCircle(primary, 6.dp.toPx(), l)
        drawCircle(peer, 6.dp.toPx(), r)
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SplitSignalColors.SurfaceCard)
            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(999.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(15.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = SplitSignalColors.TextPrimary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubbles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SystemEventPill(message: ChatMessageRecord) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(SplitSignalColors.SurfaceCard)
                .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "[ SYSTEM ]",
                style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = SplitSignalColors.TextMuted,
            )
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SplitSignalColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageRecord) {
    val alignment = if (message.isFromSelf) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromSelf) SplitSignalColors.AccentIndigo else SplitSignalColors.SurfaceCard
    val borderColor = if (message.isFromSelf) Color.Transparent else SplitSignalColors.BorderSubtle
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!message.isFromSelf) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SplitSignalColors.TextSecondary,
                )
            }
            Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = SplitSignalColors.TextPrimary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Friend request banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FriendRequestBanner(
    request: SyncPacket.FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitSignalColors.SurfaceCard)
            .border(1.dp, SplitSignalColors.BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = "Friend request",
                tint = SplitSignalColors.AccentIndigo,
                modifier = Modifier.size(16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "${request.fromName} wants to connect",
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = request.fromUserId,
                    style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                    color = SplitSignalColors.TextSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MatiksChipButton(label = "Decline", accentColor = SplitSignalColors.TextSecondary, onClick = onDecline)
            MatiksChipButton(label = "Accept", accentColor = SplitSignalColors.AccentEmerald, onClick = onAccept)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// File transfer card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FileTransferCard(
    transfer: FileTransferItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onShareFile: (filePath: String, mimeType: String) -> Unit,
) {
    var isSaved by remember(transfer.transferId) { mutableStateOf(false) }
    val alignment = if (transfer.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val progress = if (transfer.fileSizeBytes > 0L) {
        (transfer.bytesTransferred.toFloat() / transfer.fileSizeBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shareTargetPath = transfer.savedPath?.takeIf { it.isNotBlank() } ?: transfer.fileName

    val fileIcon = when {
        transfer.mimeType.startsWith("image/") ||
            transfer.fileName.endsWith(".png") ||
            transfer.fileName.endsWith(".jpg") -> Icons.Rounded.Image
        transfer.mimeType == "application/pdf" ||
            transfer.fileName.endsWith(".pdf") -> Icons.Rounded.PictureAsPdf
        else -> Icons.Rounded.FolderZip
    }
    val iconAccent = when {
        transfer.mimeType.startsWith("image/") ||
            transfer.fileName.endsWith(".png") ||
            transfer.fileName.endsWith(".jpg") -> SplitSignalColors.AccentIndigo
        transfer.mimeType == "application/pdf" ||
            transfer.fileName.endsWith(".pdf") -> SplitSignalColors.AccentCrimson
        else -> SplitSignalColors.AccentIndigo
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        LinearCard(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            borderColor = when (transfer.status) {
                TransferStatus.FAILED -> SplitSignalColors.AccentCrimson.copy(alpha = 0.5f)
                TransferStatus.COMPLETED -> SplitSignalColors.AccentEmerald.copy(alpha = 0.4f)
                else -> SplitSignalColors.BorderSubtle
            },
            contentPadding = 12.dp,
        ) {
            // File header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(iconAccent.copy(alpha = 0.14f))
                        .border(1.dp, iconAccent.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = "File",
                        tint = iconAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = transfer.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = SplitSignalColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatByteSize(transfer.fileSizeBytes)} · ${if (transfer.isOutgoing) "OUTGOING" else "INCOMING"}",
                        style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp),
                        color = SplitSignalColors.TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (transfer.status) {
                TransferStatus.PENDING_OFFER -> {
                    if (!transfer.isOutgoing) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MatiksChipButton(label = "Decline", accentColor = SplitSignalColors.AccentCrimson, onClick = onDecline)
                                MatiksChipButton(label = "Accept", accentColor = SplitSignalColors.AccentEmerald, onClick = onAccept)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Waiting for peer...",
                                style = MaterialTheme.typography.bodySmall,
                                color = SplitSignalColors.TextSecondary,
                            )
                            MatiksChipButton(label = "Cancel", accentColor = SplitSignalColors.TextSecondary, onClick = onCancel)
                        }
                    }
                }
                TransferStatus.TRANSFERRING -> {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = SplitSignalColors.AccentIndigo,
                        trackColor = SplitSignalColors.SurfaceTile,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${formatByteSize(transfer.bytesTransferred)} / ${formatByteSize(transfer.fileSizeBytes)} · ${formatByteSize(transfer.speedBytesPerSec)}/s",
                            style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
                            color = SplitSignalColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        MatiksChipButton(label = "Cancel", accentColor = SplitSignalColors.AccentCrimson, onClick = onCancel)
                    }
                }
                TransferStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (transfer.savedPath.isNullOrBlank()) "Ready" else "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = SplitSignalColors.AccentEmerald,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Save chip
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSaved) SplitSignalColors.AccentEmerald.copy(alpha = 0.14f)
                                        else SplitSignalColors.SurfaceTile,
                                    )
                                    .border(
                                        1.dp,
                                        if (isSaved) SplitSignalColors.AccentEmerald.copy(alpha = 0.55f)
                                        else SplitSignalColors.BorderSubtle,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(role = Role.Button) { isSaved = true }
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Download,
                                    contentDescription = if (isSaved) "Saved" else "Save",
                                    tint = if (isSaved) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = if (isSaved) "Saved" else "Save",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSaved) SplitSignalColors.AccentEmerald else SplitSignalColors.TextPrimary,
                                )
                            }
                            // Share chip
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SplitSignalColors.SurfaceTile)
                                    .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(8.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(role = Role.Button) { onShareFile(shareTargetPath, transfer.mimeType) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = SplitSignalColors.AccentIndigo,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SplitSignalColors.TextPrimary,
                                )
                            }
                        }
                    }
                }
                TransferStatus.FAILED -> {
                    Text(
                        text = if (transfer.errorMessage.isNullOrBlank()) "Failed" else "Failed: ${transfer.errorMessage}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.AccentCrimson,
                        maxLines = 2,
                    )
                }
                else -> {
                    Text(
                        text = transfer.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Simple input bar: [ attach ] [ text field ] [ send ]
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SimpleInputBar(
    draft: String,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onAttachFile: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitSignalColors.BgCanvas)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Attach button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SplitSignalColors.SurfaceCard)
                .border(1.dp, SplitSignalColors.BorderSubtle, CircleShape)
                .let {
                    if (enabled) it.pointerHoverIcon(PointerIcon.Hand).clickable(role = Role.Button, onClick = onAttachFile)
                    else it
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AttachFile,
                contentDescription = "Attach file",
                tint = if (enabled) SplitSignalColors.TextSecondary else SplitSignalColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }

        // Text field
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            enabled = enabled,
            singleLine = false,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SplitSignalColors.TextPrimary),
            cursorBrush = SolidColor(SplitSignalColors.AccentIndigo),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(SplitSignalColors.SurfaceCard)
                        .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                ) {
                    if (draft.isEmpty()) {
                        Text(
                            text = if (enabled) "Type a message..." else "Connect to chat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SplitSignalColors.TextMuted,
                        )
                    }
                    inner()
                }
            },
        )

        // Send button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (draft.isNotBlank() && enabled) SplitSignalColors.AccentIndigo
                    else SplitSignalColors.SurfaceCard,
                )
                .border(
                    1.dp,
                    if (draft.isNotBlank() && enabled) SplitSignalColors.AccentIndigo
                    else SplitSignalColors.BorderSubtle,
                    CircleShape,
                )
                .let {
                    if (draft.isNotBlank() && enabled) {
                        it.pointerHoverIcon(PointerIcon.Hand).clickable(role = Role.Button, onClick = onSend)
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Send",
                tint = if (draft.isNotBlank() && enabled) SplitSignalColors.TextPrimary else SplitSignalColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
