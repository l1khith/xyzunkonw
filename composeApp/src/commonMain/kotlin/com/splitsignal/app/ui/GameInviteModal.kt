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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.SyncPacket

/**
 * Full-screen Linear-inspired overlay modal for an incoming peer duel challenge.
 */
@Composable
fun GameInviteModal(
    invite: SyncPacket.GameInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdropInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplitSignalColors.BgCanvas.copy(alpha = 0.88f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = {},
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LinearCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            borderColor = SplitSignalColors.BorderSubtle,
            contentPadding = 22.dp,
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
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Duel challenge",
                        tint = SplitSignalColors.AccentIndigo,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "INCOMING CHALLENGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.AccentIndigo,
                    )
                    Text(
                        text = invite.gameType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = SplitSignalColors.TextPrimary,
                    )
                }
            }

            val challengeHeadline = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = SplitSignalColors.TextPrimary)) {
                    append(invite.fromName)
                }
                append(" (")
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                        color = SplitSignalColors.TextSecondary,
                    ),
                ) {
                    append(invite.fromUserId)
                }
                append(") challenges you to ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = SplitSignalColors.TextPrimary)) {
                    append(invite.gameType.displayName)
                }
                append("!")
            }

            Text(
                text = challengeHeadline,
                style = MaterialTheme.typography.bodyLarge,
                color = SplitSignalColors.TextSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InviteMetaPill(
                    label = "ID",
                    value = invite.fromUserId,
                    modifier = Modifier.weight(1f),
                )
                InviteMetaPill(
                    label = "SEED",
                    value = "#${invite.seed}",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TactileTile(
                    onClick = onDecline,
                    backgroundColor = SplitSignalColors.SurfaceTile,
                    restingBorderColor = SplitSignalColors.BorderSubtle,
                    cornerRadius = 10.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                ) {
                    Text(
                        text = "Decline",
                        style = MaterialTheme.typography.labelLarge,
                        color = SplitSignalColors.TextPrimary,
                    )
                }

                TactileTile(
                    onClick = onAccept,
                    backgroundColor = SplitSignalColors.AccentIndigo,
                    restingBorderColor = Color(0x33FFFFFF),
                    cornerRadius = 10.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                ) {
                    Text(
                        text = "Accept",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = SplitSignalColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteMetaPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SplitSignalColors.SurfaceTile)
            .border(1.dp, SplitSignalColors.BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SplitSignalColors.TextMuted,
        )
        Text(
            text = value,
            style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
            color = SplitSignalColors.TextSecondary,
        )
    }
}
