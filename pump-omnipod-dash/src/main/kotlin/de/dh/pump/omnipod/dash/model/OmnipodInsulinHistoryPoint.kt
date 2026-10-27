package de.dh.pump.omnipod.dash.model

import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinCategory
import de.dh.daps.common.domain.model.therapy.InsulinHistoryPoint
import de.dh.daps.common.domain.model.therapy.Timestamp

data class OmnipodInsulinHistoryPoint(
    override val timestamp: Timestamp,
    override val amount: InsulinAmount,
    override val category: InsulinCategory,
    override val pumpId: String? = null
) : InsulinHistoryPoint