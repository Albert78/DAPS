package de.dh.daps.common.model

import de.dh.daps.common.model.data.Timestamp
import java.time.LocalDate

/**
 * Expiration date representation for replaceable components or lifecycle states.
 */
sealed interface ExpirationDate {
    data class Approximate(
        val date: LocalDate
    ) : ExpirationDate

    data class Hard(
        val dateTime: Timestamp
    ) : ExpirationDate
}

/**
 * Replaceable consumable or component type on an insulin pump.
 */
enum class PumpReplaceableComponent {
    CATHETER,
    PATCH
}

/**
 * Phase of a disposable patch pump lifecycle.
 */
enum class PatchPhase {
    /**
     * Patch pump selected, initialized, or undergoing priming/insertion sequence.
     */
    PRIMING,

    /**
     * Active normal service life (e.g., 0 to 72 hours). Delivery active.
     */
    ACTIVE,

    /**
     * Grace period past active service life (e.g., 72 to 80 hours).
     * Delivery still active, but replacement required soon.
     */
    IN_GRACE_PERIOD,

    /**
     * Hard expiration reached or pod deactivated (e.g., past 80 hours, empty reservoir, occlusion, fault).
     * Delivery stopped.
     */
    EXPIRED
}

/**
 * Reason for patch pump deactivation or expiration.
 */
enum class PatchDeactivationReason {
    HARD_TIME_LIMIT,
    RESERVOIR_EMPTY,
    OCCLUSION,
    HARDWARE_FAULT,
    MANUAL_DEACTIVATION
}

/**
 * Closed domain model representing the lifecycle state of an insulin pump hardware or its consumables.
 */
sealed interface PumpLifecycleState {
    /**
     * Durable / traditional pump lifecycle (e.g. Dana-i, Accu-Chek).
     * Consumables (catheter, reservoir) are tracked as independent replaceable items with approximate expiration dates.
     */
    data class Durable(
        val approximateCatheterExpiration: LocalDate? = null,
        val approximateReservoirExpiration: LocalDate? = null,
        val catheterLastChanged: Timestamp? = null,
        val reservoirLastChanged: Timestamp? = null
    ) : PumpLifecycleState

    /**
     * Disposable Patch Pump lifecycle (e.g. Omnipod DASH, Medtrum).
     * The patch pump itself is a single-use unit with an integrated reservoir, cannula, and timer.
     */
    data class Patch(
        val phase: PatchPhase,
        val activationTime: Timestamp,
        val activeUntil: Timestamp,
        val graceUntil: Timestamp,
        val deactivationReason: PatchDeactivationReason? = null
    ) : PumpLifecycleState {
        val isOperating: Boolean
            get() = phase == PatchPhase.ACTIVE || phase == PatchPhase.IN_GRACE_PERIOD

        val isInGracePeriod: Boolean
            get() = phase == PatchPhase.IN_GRACE_PERIOD

        val isExpired: Boolean
            get() = phase == PatchPhase.EXPIRED

        /**
         * Progress fraction of normal active service life from 0.0 (just activated) to 1.0 (active service life completed).
         */
        fun activeProgress(now: Timestamp = Timestamp.now()): Float {
            val totalMs = activeUntil.ms - activationTime.ms
            if (totalMs <= 0) return 1f
            val elapsedMs = now.ms - activationTime.ms
            return (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)
        }

        /**
         * Operational time remaining in milliseconds before hard shutdown (end of grace period).
         */
        fun timeRemainingMs(now: Timestamp = Timestamp.now()): Long {
            return (graceUntil.ms - now.ms).coerceAtLeast(0L)
        }
    }
}