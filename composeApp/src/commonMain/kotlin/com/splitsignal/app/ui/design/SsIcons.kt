package com.splitsignal.app.ui.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import splitsignal.composeapp.generated.resources.ic_arrow_back
import splitsignal.composeapp.generated.resources.ic_attach_file
import splitsignal.composeapp.generated.resources.ic_attach_money
import splitsignal.composeapp.generated.resources.ic_calendar_today
import splitsignal.composeapp.generated.resources.ic_check
import splitsignal.composeapp.generated.resources.ic_check_circle
import splitsignal.composeapp.generated.resources.ic_close
import splitsignal.composeapp.generated.resources.ic_download
import splitsignal.composeapp.generated.resources.ic_forum
import splitsignal.composeapp.generated.resources.ic_local_fire_department
import splitsignal.composeapp.generated.resources.ic_monitoring
import splitsignal.composeapp.generated.resources.ic_notifications_active
import splitsignal.composeapp.generated.resources.ic_person
import splitsignal.composeapp.generated.resources.ic_person_add
import splitsignal.composeapp.generated.resources.ic_person_remove
import splitsignal.composeapp.generated.resources.ic_play_arrow
import splitsignal.composeapp.generated.resources.ic_radio_button_unchecked
import splitsignal.composeapp.generated.resources.ic_receipt_long
import splitsignal.composeapp.generated.resources.ic_schedule
import splitsignal.composeapp.generated.resources.ic_search
import splitsignal.composeapp.generated.resources.ic_send
import splitsignal.composeapp.generated.resources.ic_settings
import splitsignal.composeapp.generated.resources.ic_settings_outline
import splitsignal.composeapp.generated.resources.ic_shield
import splitsignal.composeapp.generated.resources.ic_sudoku_grid
import splitsignal.composeapp.generated.resources.ic_cross_math
import splitsignal.composeapp.generated.resources.ic_kenken
import splitsignal.composeapp.generated.resources.ic_mindsnap
import splitsignal.composeapp.generated.resources.ic_sports_esports
import splitsignal.composeapp.generated.resources.ic_square
import splitsignal.composeapp.generated.resources.ic_star
import splitsignal.composeapp.generated.resources.ic_trending_up
import splitsignal.composeapp.generated.resources.ic_wifi
import splitsignal.composeapp.generated.resources.Res

/**
 * Central icon registry. Every pictogram is a vector asset, sourced either from
 * the imported CashBuddy `composeResources/drawable` set or from Material Icons.
 * No emoji glyphs are used in production UI.
 */
object SsIcons {
    val Back: DrawableResource = Res.drawable.ic_arrow_back
    val Play: DrawableResource = Res.drawable.ic_play_arrow
    val Games: DrawableResource = Res.drawable.ic_sports_esports
    val Network: DrawableResource = Res.drawable.ic_wifi
    val Profile: DrawableResource = Res.drawable.ic_person
    val Chat: DrawableResource = Res.drawable.ic_forum
    val Send: DrawableResource = Res.drawable.ic_send
    val Attach: DrawableResource = Res.drawable.ic_attach_file
    val StreakFire: DrawableResource = Res.drawable.ic_local_fire_department
    val Coin: DrawableResource = Res.drawable.ic_attach_money
    val Shield: DrawableResource = Res.drawable.ic_shield
    val Check: DrawableResource = Res.drawable.ic_check
    val CheckCircle: DrawableResource = Res.drawable.ic_check_circle
    val Close: DrawableResource = Res.drawable.ic_close
    val Circle: DrawableResource = Res.drawable.ic_radio_button_unchecked
    val Square: DrawableResource = Res.drawable.ic_square
    val AddFriend: DrawableResource = Res.drawable.ic_person_add
    val RemoveFriend: DrawableResource = Res.drawable.ic_person_remove
    val Search: DrawableResource = Res.drawable.ic_search
    val Notifications: DrawableResource = Res.drawable.ic_notifications_active
    val Settings: DrawableResource = Res.drawable.ic_settings
    val SettingsOutline: DrawableResource = Res.drawable.ic_settings_outline
    val SudokuGrid: DrawableResource = Res.drawable.ic_sudoku_grid
    val CrossMath: DrawableResource = Res.drawable.ic_cross_math
    val KenKen: DrawableResource = Res.drawable.ic_kenken
    val MindSnap: DrawableResource = Res.drawable.ic_mindsnap
    val Trophy: DrawableResource = Res.drawable.ic_star
    val TrendUp: DrawableResource = Res.drawable.ic_trending_up
    val Schedule: DrawableResource = Res.drawable.ic_schedule
    val Calendar: DrawableResource = Res.drawable.ic_calendar_today
    val Analytics: DrawableResource = Res.drawable.ic_monitoring
    val Receipt: DrawableResource = Res.drawable.ic_receipt_long
    val Download: DrawableResource = Res.drawable.ic_download

    val MaterialAdd: ImageVector = Icons.Rounded.Add
    val MaterialArrowUpward: ImageVector = Icons.Rounded.ArrowUpward
    val MaterialCheck: ImageVector = Icons.Rounded.Check
    val MaterialCheckCircle: ImageVector = Icons.Rounded.CheckCircle
    val MaterialClose: ImageVector = Icons.Rounded.Close
    val MaterialDownload: ImageVector = Icons.Rounded.Download
    val MaterialFolderZip: ImageVector = Icons.Rounded.FolderZip
    val MaterialImage: ImageVector = Icons.Rounded.Image
    val MaterialNotifications: ImageVector = Icons.Rounded.Notifications
    val MaterialPerson: ImageVector = Icons.Rounded.Person
    val MaterialPdf: ImageVector = Icons.Rounded.PictureAsPdf
    val MaterialRadioUnchecked: ImageVector = Icons.Rounded.RadioButtonUnchecked
    val MaterialSearch: ImageVector = Icons.Rounded.Search
    val MaterialSettings: ImageVector = Icons.Rounded.Settings
    val MaterialShare: ImageVector = Icons.Rounded.Share
    val MaterialSportsEsports: ImageVector = Icons.Rounded.SportsEsports
    val MaterialSquare: ImageVector = Icons.Rounded.Square
    val MaterialWifi: ImageVector = Icons.Rounded.Wifi
    val MaterialWifiOff: ImageVector = Icons.Rounded.WifiOff
}

@Composable
fun SsIcon(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = SsColors.TextPrimary,
    iconSize: Dp = 18.dp,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(iconSize),
    )
}

@Composable
fun SsIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = SsColors.TextPrimary,
    iconSize: Dp = 18.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(iconSize),
    )
}
