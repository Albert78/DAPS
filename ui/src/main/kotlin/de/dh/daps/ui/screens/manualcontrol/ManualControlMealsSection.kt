package de.dh.daps.ui.screens.manualcontrol

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealEntry
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.getDefaultSlowMealType
import de.dh.daps.common.model.getDefaultStandardMealType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.carbsValue
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.timeWithUnit
import de.dh.daps.ui.screens.mealtypes.MealTypeIcon

@Composable
fun ManualControlMealsSection(
    lastPastMeal: MealEntry?,
    nextPlannedMeal: MealEntry?,
    hasNextPlannedMealReminder: Boolean = false,
    onEditMeal: (Long) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(id = R.string.manual_control_meals_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Last Past Meal (Compact)
            CompactMealInfoCard(
                title = stringResource(id = R.string.manual_control_past_meal_title),
                mealEntry = lastPastMeal,
                emptyText = stringResource(id = R.string.manual_control_no_past_meal, ManualControlViewModel.PAST_MEAL_LOOKBACK_HOURS),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = lastPastMeal?.let { meal -> { onEditMeal(meal.id) } }
            )

            // Next Planned Meal (Compact)
            CompactMealInfoCard(
                title = stringResource(id = R.string.manual_control_next_meal_title),
                mealEntry = nextPlannedMeal,
                emptyText = stringResource(id = R.string.manual_control_no_next_meal),
                hasReminder = hasNextPlannedMealReminder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = nextPlannedMeal?.let { meal -> { onEditMeal(meal.id) } }
            )
        }
    }
}

@Composable
fun CompactMealInfoCard(
    title: String,
    mealEntry: MealEntry?,
    emptyText: String,
    modifier: Modifier = Modifier,
    hasReminder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    OutlinedCard(
        onClick = { onClick?.invoke() },
        enabled = mealEntry != null && onClick != null,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasReminder && mealEntry != null) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(id = R.string.meal_correction_bolus_reminder_label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (mealEntry != null) {
                val carbText = carbsValue(mealEntry.carbGrams)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = carbText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        MealTypeIcon(
                            mealType = mealEntry.mealType,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = timeWithUnit(mealEntry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mealEntry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mealEntry.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Meals Section Preview")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Meals Section - Dark Mode")
@Composable
fun ManualControlMealsSectionPreview() {
    AppPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            ManualControlMealsSection(
                lastPastMeal = MealEntry(
                    id = 1L,
                    timestamp = Timestamp.now().minusHours(2),
                    carbGrams = 45.0,
                    mealType = getDefaultStandardMealType(LocalContext.current),
                    administeredInsulinAmount = InsulinAmount(3.5)
                ),
                nextPlannedMeal = MealEntry(
                    id = 2L,
                    timestamp = Timestamp.now().plusHours(3),
                    carbGrams = 60.0,
                    mealType = getDefaultSlowMealType(LocalContext.current),
                    description = "Pizza"
                ),
                hasNextPlannedMealReminder = true,
                onEditMeal = {}
            )
        }
    }
}