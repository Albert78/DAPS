package de.dh.pump.omnipod.dash.model

import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinCategory
import de.dh.daps.common.model.InsulinHistoryPoint
import de.dh.daps.common.model.data.Timestamp

data class OmnipodInsulinHistoryPoint(
    override val timestamp: Timestamp,
    override val amount: InsulinAmount,
    override val category: InsulinCategory,
    override val pumpId: String? = null
) : InsulinHistoryPoint