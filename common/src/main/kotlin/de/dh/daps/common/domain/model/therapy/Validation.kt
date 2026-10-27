package de.dh.daps.common.domain.model.therapy

import de.dh.daps.common.ADJUSTMENT_PERCENTAGE_MAX
import de.dh.daps.common.ADJUSTMENT_PERCENTAGE_MIN
import de.dh.daps.common.BASAL_MAX
import de.dh.daps.common.BASAL_MIN
import de.dh.daps.common.BG_DELTA_MAX
import de.dh.daps.common.BOLUS_MAX
import de.dh.daps.common.BOLUS_MIN
import de.dh.daps.common.CARBS_GRAMS_MAX
import de.dh.daps.common.CARBS_GRAMS_MIN
import de.dh.daps.common.CR_MAX
import de.dh.daps.common.CR_MIN
import de.dh.daps.common.ISF_MAX
import de.dh.daps.common.ISF_MIN
import de.dh.daps.common.LOW_THRESHOLD_MAX
import de.dh.daps.common.LOW_THRESHOLD_MIN
import de.dh.daps.common.TARGET_MAX
import de.dh.daps.common.TARGET_MIN
import de.dh.daps.common.domain.model.data.Block
import kotlin.math.abs

fun isValidBasal(rate: Double): Boolean = rate in BASAL_MIN..BASAL_MAX

fun isValidIsf(isf: Double): Boolean = isf in ISF_MIN..ISF_MAX

fun isValidCr(cr: Double): Boolean = cr in CR_MIN..CR_MAX

fun isValidTarget(target: Short): Boolean = target in TARGET_MIN..TARGET_MAX

fun isValidLowThreshold(threshold: Short): Boolean = threshold in LOW_THRESHOLD_MIN..LOW_THRESHOLD_MAX

fun isValidAdjustmentPercentage(percentage: Int): Boolean = percentage in ADJUSTMENT_PERCENTAGE_MIN..ADJUSTMENT_PERCENTAGE_MAX

fun validateBasalBlocks(blocks: List<Block>): Boolean =
    blocks.all { isValidBasal(it.amount) }

fun validateIsfBlocks(blocks: List<Block>): Boolean =
    blocks.all { isValidIsf(it.amount) }

fun validateCrBlocks(blocks: List<Block>): Boolean =
    blocks.all { isValidCr(it.amount) }

fun validateIsf(isf: Double) {
    require(isValidIsf(isf)) { "ISF out of range: $isf" }
}

fun validateCr(cr: Double) {
    require(isValidCr(cr)) { "CR out of range: $cr" }
}

fun validateBgDelta(delta: Double) {
    require(abs(delta) <= BG_DELTA_MAX) { "BG Delta out of range: $delta" }
}

fun validateInsulin(iu: Double) {
    require(iu in BOLUS_MIN..BOLUS_MAX) { "Insulin out of range: $iu" }
}

fun validateCarbs(carbs: Double) {
    require(carbs in CARBS_GRAMS_MIN..CARBS_GRAMS_MAX) { "Carbs out of range: $carbs" }
}