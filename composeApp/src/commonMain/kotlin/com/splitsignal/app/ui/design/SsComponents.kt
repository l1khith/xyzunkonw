package com.splitsignal.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.splitsignal.app.ui.LinearTextStyles
import com.splitsignal.app.ui.TactileTile
import org.jetbrains.compose.resources.DrawableResource

enum class SsButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Dangerous,
}

enum class SsBadgeTone {
    Neutral,
    Accent,
    Success,
    Warning,
    Danger,
}

data class SsNavItem(
    val label: String,
    val icon: DrawableResource,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun SsCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SsColors.BorderSubtle,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(SsRadii.Card)
            .background(SsColors.SurfaceCard)
            .border(1.dp, borderColor, SsRadii.Card)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SsButtonVariant = SsButtonVariant.Primary,
    enabled: Boolean = true,
    icon: DrawableResource? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
) {
    val background = when (variant) {
        SsButtonVariant.Primary -> SsColors.AccentIndigo
        SsButtonVariant.Secondary -> SsColors.SurfaceTile
        SsButtonVariant.Ghost -> SsColors.SurfaceCard
        SsButtonVariant.Dangerous -> SsColors.AccentCrimson
    }
    val border = when (variant) {
        SsButtonVariant.Primary -> SsColors.AccentIndigo
        SsButtonVariant.Secondary -> SsColors.BorderFocus
        SsButtonVariant.Ghost -> SsColors.BorderSubtle
        SsButtonVariant.Dangerous -> SsColors.AccentCrimson
    }
    val contentColor = if (enabled) SsColors.TextPrimary else SsColors.TextMuted

    TactileTile(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = background,
        restingBorderColor = border,
        cornerRadius = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                SsIcon(icon = icon, contentDescription = null, tint = contentColor, iconSize = 14.dp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SsIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SsColors.TextPrimary,
    background: Color = SsColors.SurfaceTile,
    buttonSize: Dp = 34.dp,
    iconSize: Dp = 16.dp,
    enabled: Boolean = true,
) {
    TactileTile(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        enabled = enabled,
        backgroundColor = background,
        restingBorderColor = SsColors.BorderSubtle,
        cornerRadius = 8.dp,
    ) {
        SsIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else SsColors.TextMuted,
            iconSize = iconSize,
        )
    }
}

@Composable
fun SsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SsColors.TextPrimary,
    background: Color = SsColors.SurfaceTile,
    buttonSize: Dp = 34.dp,
    iconSize: Dp = 16.dp,
    enabled: Boolean = true,
) {
    TactileTile(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        enabled = enabled,
        backgroundColor = background,
        restingBorderColor = SsColors.BorderSubtle,
        cornerRadius = 8.dp,
    ) {
        SsIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else SsColors.TextMuted,
            iconSize = iconSize,
        )
    }
}

@Composable
fun SsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: DrawableResource? = null,
    accentColor: Color = SsColors.AccentIndigo,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.16f) else SsColors.SurfaceTile,
        animationSpec = tween(150),
        label = "SsChipBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accentColor else SsColors.TextSecondary,
        animationSpec = tween(150),
        label = "SsChipContent",
    )

    TactileTile(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = backgroundColor,
        restingBorderColor = if (selected) accentColor.copy(alpha = 0.45f) else SsColors.BorderSubtle,
        cornerRadius = 999.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                SsIcon(icon = leadingIcon, contentDescription = null, tint = contentColor, iconSize = 14.dp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = LinearTextStyles.TabularLabel.copy(fontSize = 11.sp),
            color = SsColors.TextSecondary,
        )
        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(role = Role.Button, onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SsColors.AccentIndigo,
                )
            }
        }
    }
}

@Composable
fun SsStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: SsBadgeTone = SsBadgeTone.Neutral,
) {
    val (background, foreground) = when (tone) {
        SsBadgeTone.Neutral -> SsColors.SurfaceTile to SsColors.TextSecondary
        SsBadgeTone.Accent -> SsColors.AccentIndigo.copy(alpha = 0.16f) to SsColors.AccentIndigo
        SsBadgeTone.Success -> SsColors.AccentEmerald.copy(alpha = 0.16f) to SsColors.AccentEmerald
        SsBadgeTone.Warning -> SsColors.AccentAmber.copy(alpha = 0.16f) to SsColors.AccentAmber
        SsBadgeTone.Danger -> SsColors.AccentCrimson.copy(alpha = 0.16f) to SsColors.AccentCrimson
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = LinearTextStyles.TabularLabel.copy(fontSize = 10.sp),
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
fun SsEmptyState(
    icon: DrawableResource,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SsColors.SurfaceTile)
                .border(1.dp, SsColors.BorderSubtle, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SsIcon(icon = icon, contentDescription = null, tint = SsColors.TextSecondary, iconSize = 26.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SsColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SsColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            SsButton(text = actionText, onClick = onActionClick)
        }
    }
}

@Composable
fun SsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SsColors.BgCanvas),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    SsIconButton(
                        icon = SsIcons.Back,
                        contentDescription = "Back",
                        onClick = onBack,
                        background = SsColors.SurfaceCard,
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SsColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        HorizontalDivider(color = SsColors.BorderSubtle, thickness = 1.dp)
    }
}

@Composable
fun SsBottomNav(
    items: List<SsNavItem>,
    onFabClick: (() -> Unit)? = null,
    fabIcon: DrawableResource = SsIcons.Play,
    fabContentDescription: String = "Primary action",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(64.dp),
            shape = RoundedCornerShape(50),
            color = SsColors.SurfaceCard,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SsColors.BorderSubtle),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    FloatingNavPillItem(item = item)
                }
            }
        }

        if (onFabClick != null) {
            Spacer(modifier = Modifier.width(12.dp))
            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                containerColor = SsColors.AccentIndigo,
                contentColor = SsColors.TextPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp,
                ),
            ) {
                SsIcon(
                    icon = fabIcon,
                    contentDescription = fabContentDescription,
                    tint = SsColors.TextPrimary,
                    iconSize = 26.dp,
                )
            }
        }
    }
}

@Composable
private fun FloatingNavPillItem(item: SsNavItem) {
    val backgroundColor by animateColorAsState(
        targetValue = if (item.selected) SsColors.AccentIndigo.copy(alpha = 0.18f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "navItemBg",
    )
    val tint by animateColorAsState(
        targetValue = if (item.selected) SsColors.AccentIndigo else SsColors.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "navItemTint",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Tab, onClick = item.onClick),
        contentAlignment = Alignment.Center,
    ) {
        SsIcon(
            icon = item.icon,
            contentDescription = item.label,
            tint = tint,
            iconSize = 24.dp,
        )
    }
}

@Composable
fun SsNavRail(
    items: List<SsNavItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(SsColors.SurfaceCard),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                NavigationItem(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    activeLine = Alignment.CenterStart,
                )
            }
        }
        Column(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(SsColors.BorderSubtle),
        ) {}
    }
}

@Composable
private fun NavigationItem(
    item: SsNavItem,
    modifier: Modifier,
    activeLine: Alignment,
) {
    val tint = if (item.selected) SsColors.TextPrimary else SsColors.TextSecondary
    Box(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(role = Role.Tab, onClick = item.onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (item.selected) {
            Box(
                modifier = Modifier
                    .align(activeLine)
                    .then(
                        if (activeLine == Alignment.CenterStart) {
                            Modifier
                                .width(2.dp)
                                .height(28.dp)
                        } else {
                            Modifier
                                .fillMaxWidth(0.55f)
                                .height(2.dp)
                        },
                    )
                    .background(SsColors.AccentIndigo),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SsIcon(
                icon = item.icon,
                contentDescription = item.label,
                tint = tint,
                iconSize = 20.dp,
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = tint,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SsLiveDot(
    modifier: Modifier = Modifier,
    color: Color = SsColors.AccentEmerald,
    dotSize: Dp = 7.dp,
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(color),
    )
}
