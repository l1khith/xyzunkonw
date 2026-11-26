package com.splitsignal.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.core.domain.BoardTheme
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.ProgressionStore
import com.splitsignal.core.domain.StreakTier

/**
 * Vault & Mastery screen showcasing player rank progression, Elo, streak status,
 * consumable items (Streak Freezes, Logical Hint Tokens), and unlockable Sudoku board themes.
 */
@Composable
fun VaultShopScreen(
    progression: ProgressionState,
    rankTitle: String,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onBuyStreakFreeze: () -> Unit,
    onBuyHintToken: () -> Unit,
    onBuyOrSelectTheme: (BoardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxContentWidth = if (widthSizeClass == WindowWidthSizeClass.Expanded) 900.dp else 560.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VaultTopBar(
            coins = progression.coins,
            rankTitle = rankTitle,
            onBack = onBack,
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MasteryRankCard(
                progression = progression,
                rankTitle = rankTitle,
            )

            ConsumablesShopSection(
                progression = progression,
                onBuyStreakFreeze = onBuyStreakFreeze,
                onBuyHintToken = onBuyHintToken,
            )

            BoardThemesSection(
                progression = progression,
                onBuyOrSelectTheme = onBuyOrSelectTheme,
            )
        }
    }
}

@Composable
private fun VaultTopBar(
    coins: Int,
    rankTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            VaultBackChip(onClick = onBack)
            Text(
                text = "Vault & Mastery",
                style = MaterialTheme.typography.titleMedium,
                color = SplitSignalColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SplitSignalColors.TileSurface)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = rankTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.AccentSand,
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SplitSignalColors.AccentEmerald.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        SplitSignalColors.AccentEmerald.copy(alpha = 0.55f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "$coins Coins",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SplitSignalColors.AccentEmerald,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MasteryRankCard(
    progression: ProgressionState,
    rankTitle: String,
) {
    val streakTier = StreakTier.forDays(progression.streakDays)
    val rankLadder = remember(progression.xp) { computeRankProgress(progression.xp) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "MASTERY RANK",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplitSignalColors.AccentEmerald,
                )
                Text(
                    text = rankTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = "Novice to Grandmaster progression",
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SplitSignalColors.BgObsidian)
                    .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${progression.elo}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SplitSignalColors.AccentEmerald,
                    )
                    Text(
                        text = "ELO RATING",
                        style = MaterialTheme.typography.labelSmall,
                        color = SplitSignalColors.TextSecondary,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${progression.xp} XP",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = rankLadder.nextMilestoneLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = SplitSignalColors.TextSecondary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SplitSignalColors.BgObsidian),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = rankLadder.progressFraction)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SplitSignalColors.AccentEmerald),
                )
            }
        }

        HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MasteryStatPill(
                label = "STREAK TIER",
                value = "${streakTier.label} (${progression.streakDays}d)",
                modifier = Modifier.weight(1f),
            )
            MasteryStatPill(
                label = "FREEZES",
                value = "${progression.streakFreezes} Ready",
                modifier = Modifier.weight(1f),
            )
            MasteryStatPill(
                label = "SOLVED",
                value = "${progression.solvedCount}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MasteryStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SplitSignalColors.BgObsidian)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.TextSecondary,
            maxLines = 1,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = SplitSignalColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConsumablesShopSection(
    progression: ProgressionState,
    onBuyStreakFreeze: () -> Unit,
    onBuyHintToken: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "UTILITY & BOOSTS",
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.AccentEmerald,
        )

        ShopItemRow(
            title = "Streak Freeze",
            description = "Shields your daily streak automatically if you miss a day.",
            ownedLabel = "Owned: ${progression.streakFreezes}",
            costCoins = ProgressionStore.STREAK_FREEZE_COST,
            canAfford = progression.coins >= ProgressionStore.STREAK_FREEZE_COST,
            onBuy = onBuyStreakFreeze,
        )

        HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)

        ShopItemRow(
            title = "Logical Hint Token",
            description = "Reveals a Naked or Hidden Single deduction in plain English.",
            ownedLabel = "Owned: ${progression.hintTokens}",
            costCoins = ProgressionStore.HINT_TOKEN_COST,
            canAfford = progression.coins >= ProgressionStore.HINT_TOKEN_COST,
            onBuy = onBuyHintToken,
        )
    }
}

@Composable
private fun ShopItemRow(
    title: String,
    description: String,
    ownedLabel: String,
    costCoins: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SplitSignalColors.TextPrimary,
                )
                Text(
                    text = ownedLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SplitSignalColors.TextSecondary,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }

        Button(
            onClick = onBuy,
            enabled = canAfford,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitSignalColors.AccentEmerald,
                contentColor = SplitSignalColors.BgObsidian,
                disabledContainerColor = SplitSignalColors.TileSurface,
                disabledContentColor = SplitSignalColors.TextSecondary,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
            modifier = Modifier.pointerHoverIcon(if (canAfford) PointerIcon.Hand else PointerIcon.Default),
        ) {
            Text(
                text = "$costCoins Coins",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BoardThemesSection(
    progression: ProgressionState,
    onBuyOrSelectTheme: (BoardTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSignalColors.CardSlate)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "BOARD THEMES",
            style = MaterialTheme.typography.labelSmall,
            color = SplitSignalColors.AccentEmerald,
        )

        BoardTheme.entries.forEachIndexed { index, theme ->
            if (index > 0) {
                HorizontalDivider(color = SplitSignalColors.BorderHairline, thickness = 1.dp)
            }
            val isEquipped = progression.activeTheme == theme
            val isUnlocked = theme in progression.unlockedThemes
            val canAfford = progression.coins >= theme.priceCoins

            BoardThemeRow(
                theme = theme,
                isEquipped = isEquipped,
                isUnlocked = isUnlocked,
                canAfford = canAfford,
                onClick = { onBuyOrSelectTheme(theme) },
            )
        }
    }
}

@Composable
private fun BoardThemeRow(
    theme: BoardTheme,
    isEquipped: Boolean,
    isUnlocked: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    val swatches = themePreviewColors(theme)
    val subtitle = when {
        theme.priceCoins == 0 -> "Free · Default dark obsidian slate"
        theme == BoardTheme.PAPER_IVORY -> "Warm editorial ivory paper with emerald ink"
        else -> "High-contrast solarized cyan & deep slate"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SplitSignalColors.BgObsidian)
                .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(8.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (color in swatches) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, SplitSignalColors.BorderHairline, CircleShape),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = theme.label,
                style = MaterialTheme.typography.titleSmall,
                color = SplitSignalColors.TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SplitSignalColors.TextSecondary,
            )
        }

        when {
            isEquipped -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SplitSignalColors.AccentEmerald.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            SplitSignalColors.AccentEmerald.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "EQUIPPED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SplitSignalColors.AccentEmerald,
                    )
                }
            }

            isUnlocked -> {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SplitSignalColors.AccentEmerald.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SplitSignalColors.TileSurface,
                        contentColor = SplitSignalColors.TextPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(
                        text = "EQUIP",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }

            else -> {
                Button(
                    onClick = onClick,
                    enabled = canAfford,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitSignalColors.AccentEmerald,
                        contentColor = SplitSignalColors.BgObsidian,
                        disabledContainerColor = SplitSignalColors.TileSurface,
                        disabledContentColor = SplitSignalColors.TextSecondary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.pointerHoverIcon(if (canAfford) PointerIcon.Hand else PointerIcon.Default),
                ) {
                    Text(
                        text = "UNLOCK · ${theme.priceCoins}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultBackChip(
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) SplitSignalColors.TileSurface else SplitSignalColors.BgObsidian)
            .border(1.dp, SplitSignalColors.BorderHairline, RoundedCornerShape(8.dp))
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "< Back",
            style = MaterialTheme.typography.labelMedium,
            color = SplitSignalColors.TextPrimary,
            maxLines = 1,
        )
    }
}

private data class RankProgress(
    val progressFraction: Float,
    val nextMilestoneLabel: String,
)

private fun computeRankProgress(xp: Int): RankProgress {
    val thresholds = listOf(0, 300, 800, 1500, 3000, 6000)
    for (i in 0 until thresholds.lastIndex) {
        val start = thresholds[i]
        val end = thresholds[i + 1]
        if (xp < end) {
            val fraction = ((xp - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
            return RankProgress(
                progressFraction = fraction,
                nextMilestoneLabel = "Next Rank: $end XP",
            )
        }
    }
    return RankProgress(
        progressFraction = 1f,
        nextMilestoneLabel = "Max Rank Reached",
    )
}

private fun themePreviewColors(theme: BoardTheme): List<Color> = when (theme) {
    BoardTheme.OBSIDIAN_DARK -> listOf(
        SplitSignalColors.BgObsidian,
        SplitSignalColors.TileSurface,
        SplitSignalColors.AccentEmerald,
    )

    BoardTheme.PAPER_IVORY -> listOf(
        Color(0xFFF6F1E7),
        Color(0xFFE5DCCB),
        Color(0xFF047857),
    )

    BoardTheme.SOLARIZED_SLATE -> listOf(
        Color(0xFF002B36),
        Color(0xFF0B414F),
        Color(0xFF2AA198),
    )
}
