package de.dh.daps.ui.common.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.StackedLineChart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.mealtypes.BadgedStarIcon

// Meal Type Icons
val Icon_Meal_Fast = Icons.Outlined.Bolt
val Icon_Meal_Standard = Icons.Outlined.Meal
val Icon_Meal_High_Fat = Icons.Outlined.Fastfood
val Icon_Meal_Slow = Icons.Outlined.Timer
val Icon_Meal_Custom = Icons.Outlined.StarOutline

// Menu and header icons
val Icon_More = Icons.Default.MoreVert
val Icon_Delete_Filled = Icons.Default.Delete
val Icon_Screen_Back = Icons.AutoMirrored.Filled.ArrowBack
val Icon_Screen_Close = Icons.Default.Clear
val Icon_Meal = Icons.Outlined.Meal
val Icon_Bolus = Icons.Outlined.Syringe
val Icon_Basal = Icons.Outlined.Basal
val Icon_Temp_Basal = Icons.Outlined.Temp_Basal
val Icon_Basal_Alternative = Icons.Outlined.Basal_Alternative
val Icon_Reservoir = Icons.Outlined.PumpReservoir
val Icon_Insulin_Profile = Icons.Outlined.StackedLineChart
val Icon_Therapy_Adjustment = Icons.Outlined.Tune
val Icon_Insulin_Adjustment = Icons.Default.UnfoldMore
val Icon_Target_Bg = Icons.Default.Adjust
val Icon_Low_Threshold = Icons.Default.VerticalAlignBottom
val Icon_Food_Database = Icons.AutoMirrored.Outlined.MenuBook
val Icon_System_Control = Icons.Outlined.Build
val Icon_Alarms = Icons.Outlined.NotificationsActive
val Icon_Permissions = Icons.Outlined.Security
val Icon_Meal_Types = Icons.Outlined.Meal_Types
val Icon_Master_Data = Icons.Outlined.Badge

val Icon_Add = Icons.Outlined.Add
val Icon_Edit = Icons.Outlined.Edit
val Icon_Info = Icons.Outlined.Info
val Icon_Clear = Icons.Outlined.Clear
val Icon_Error = Icons.Outlined.Error
val Icon_Config = Icons.Outlined.Config_Outline
val Icon_Delete = Icons.Outlined.Delete
val Icon_Archive = Icons.Outlined.Archive
val Icon_Warning = Icons.Outlined.Warning
val Icon_Arrow_Up = Icons.Outlined.ArrowUpward
val Icon_Check_No = Icons.Outlined.Close
val Icon_Comments = Icons.AutoMirrored.Outlined.Comment
val Icon_Settings = Icons.Outlined.Settings
val Icon_Check_Yes = Icons.Outlined.Check
val Icon_Arrow_Down = Icons.Outlined.ArrowDownward
val Icon_Next = Icons.Filled.Next
val Icon_Previous = Icons.Filled.Previous
val Icon_Plus = Icons.Filled.Plus
val Icon_Minus = Icons.Filled.Minus
val Icon_Alarm_Snooze = Icons.Outlined.Snooze
val Icon_Theme_Light_Dark = Icons.Outlined.Theme_Light_Dark
val Icon_Scrollview_Arrow_Up = Icons.Outlined.KeyboardArrowUp
val Icon_Scrollview_Arrow_Down = Icons.Outlined.KeyboardArrowDown
val Icon_Ui = Icons.Outlined.Palette
val Icon_Backup = Icons.Outlined.Backup
val Icon_Restore = Icons.Outlined.Restore
val Icon_Carbs = Icons.Filled.Carbs
val Icon_Insulin = Icons.Filled.Insulin
val Icon_Carbs_Blood = Icons.Filled.CarbsBlood
val Icon_Insulin_Blood = Icons.Filled.InsulinBlood
val Icon_Sound_Vibration = Icons.Outlined.SoundVibration
val Icon_Sound_Only = Icons.Outlined.SoundOnly
val Icon_Vibration_Only = Icons.Outlined.VibrationOnly
val Icon_Sound_Off = Icons.Outlined.SoundOff

// Mode & Control Icons
val Icon_ManualControlMode = Icons.Outlined.ManualControl

private data class IconPreview(
    val name: String,
    val imageVector: ImageVector
)

private val iconsForPreview = listOf(
    IconPreview("Meal_Fast", Icon_Meal_Fast),
    IconPreview("Meal_Standard", Icon_Meal_Standard),
    IconPreview("Meal_High_Fat", Icon_Meal_High_Fat),
    IconPreview("Meal_Slow", Icon_Meal_Slow),
    IconPreview("Meal_Custom", Icon_Meal_Custom),

    IconPreview("ManualControlMode", Icon_ManualControlMode),
    IconPreview("Meal", Icon_Meal),
    IconPreview("Bolus", Icon_Bolus),
    IconPreview("Basal", Icon_Basal),
    IconPreview("Temp_Basal", Icon_Temp_Basal),
    IconPreview("Basal_Alternative", Icon_Basal_Alternative),
    IconPreview("Reservoir", Icon_Reservoir),
    IconPreview("Insulin_Profile", Icon_Insulin_Profile),
    IconPreview("Therapy_Adjustment", Icon_Therapy_Adjustment),
    IconPreview("Insulin_Adjustment", Icon_Insulin_Adjustment),
    IconPreview("Target_Bg", Icon_Target_Bg),
    IconPreview("Low_Threshold", Icon_Low_Threshold),
    IconPreview("Food_Database", Icon_Food_Database),
    IconPreview("System_Control", Icon_System_Control),
    IconPreview("Alarms", Icon_Alarms),
    IconPreview("Permissions", Icon_Permissions),
    IconPreview("Meal_Types", Icon_Meal_Types),
    IconPreview("Master_Data", Icon_Master_Data),
    IconPreview("More", Icon_More),
    IconPreview("Delete_Filled", Icon_Delete_Filled),
    IconPreview("Icon_Screen_Back", Icon_Screen_Back),
    IconPreview("Icon_Screen_Close", Icon_Screen_Close),
    IconPreview("Add", Icon_Add),
    IconPreview("Edit", Icon_Edit),
    IconPreview("Info", Icon_Info),
    IconPreview("Clear", Icon_Clear),
    IconPreview("Error", Icon_Error),
    IconPreview("Config", Icon_Config),
    IconPreview("Delete", Icon_Delete),
    IconPreview("Archive", Icon_Archive),
    IconPreview("Warning", Icon_Warning),
    IconPreview("Arrow_Up", Icon_Arrow_Up),
    IconPreview("Check_No", Icon_Check_No),
    IconPreview("Comments", Icon_Comments),
    IconPreview("Settings", Icon_Settings),
    IconPreview("Check_Yes", Icon_Check_Yes),
    IconPreview("Arrow_Down", Icon_Arrow_Down),
    IconPreview("Alarm_Snooze", Icon_Alarm_Snooze),
    IconPreview("Theme_Light_Dark", Icon_Theme_Light_Dark),
    IconPreview("Scrollview_Arrow_Up", Icon_Scrollview_Arrow_Up),
    IconPreview("Scrollview_Arrow_Down", Icon_Scrollview_Arrow_Down),
    IconPreview("Ui", Icon_Ui),
    IconPreview("Backup", Icon_Backup),
    IconPreview("Restore", Icon_Restore),
    IconPreview("Carbs", Icon_Carbs),
    IconPreview("Insulin", Icon_Insulin),
    IconPreview("Carbs_Blood", Icon_Carbs_Blood),
    IconPreview("Insulin_Blood", Icon_Insulin_Blood),
    IconPreview("Next", Icon_Next),
    IconPreview("Previous", Icon_Previous),
    IconPreview("Plus", Icon_Plus),
    IconPreview("Minus", Icon_Minus),
    IconPreview("Sound_Vibration", Icon_Sound_Vibration),
    IconPreview("Sound_Only", Icon_Sound_Only),
    IconPreview("Vibration_Only", Icon_Vibration_Only),
    IconPreview("Sound_Off", Icon_Sound_Off)
)

@Preview(showBackground = true, widthDp = 320, heightDp = 1900, name = "Icon Catalog")
@Composable
fun IconCatalogPreview() {
    AppTheme {
        Surface {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(text = "Icon",
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = "Name",
                            modifier = Modifier
                                .weight(3f)
                                .padding(start = 16.dp)
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("M" to "Custom Meal M", "3" to "Custom Meal 3").forEach { (label, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BadgedStarIcon(text = label)
                                }
                                Text(
                                    text = name,
                                    modifier = Modifier
                                        .weight(3f)
                                        .padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
                items(iconsForPreview) { preview ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = preview.imageVector,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .weight(1f)
                        )
                        Text(
                            preview.name, modifier = Modifier
                                .weight(3f)
                                .padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}