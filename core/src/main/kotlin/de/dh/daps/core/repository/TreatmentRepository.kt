package de.dh.daps.core.repository

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.DeferredBolus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinApplication
import de.dh.daps.common.domain.model.therapy.InsulinCategory
import de.dh.daps.common.domain.model.therapy.InsulinHistory
import de.dh.daps.common.domain.model.therapy.InsulinOrigin
import de.dh.daps.common.domain.model.therapy.InsulinStatus
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.MealEntry
import de.dh.daps.common.domain.model.therapy.MealType
import de.dh.daps.core.aps.STALE_DEFERRED_BOLUS_THRESHOLD
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.repository.db.dao.MetabolicEventsDao
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

/**
 * Repository for active metabolic treatments (Insulin and Meals).
 * It maintains an in-memory cache for fast access by the APS core and handles persistence via DAOs.
 */
class TreatmentRepository(
    /**
     * Size of the in-memory caches. This does not affect persistence in the database.
     */
    val historySize: Minutes,
    appDatabase: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val metabolicEventsDao: MetabolicEventsDao = appDatabase.metabolicEventsDao()

    val mealTypesMapState: StateFlow<Map<String, MealType>> = metabolicEventsDao.observeAllMealTypes()
        .map { entities ->
            entities.map { it.toModel() }
                .sortedWith(compareBy({ it.sortOrder }, { it.name }))
                .associateBy { it.id }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val insulinTypesMapState: StateFlow<Map<String, InsulinType>> = metabolicEventsDao.observeAllInsulinTypes()
        .map { list ->
            list.map { it.toModel() }
                .sortedBy { it.name }
                .associateBy { it.id }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val mealsState: StateFlow<List<MealEntry>> = combine(
        metabolicEventsDao.observeMealsSince(historyStart().ms),
        mealTypesMapState
    ) { entities, mealTypesMap ->
        entities.mapNotNull { entity ->
            mealTypesMap[entity.meal_type_id]?.let { type -> entity.toModel(type) }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val insulinApplicationsState: StateFlow<List<InsulinApplication>> = combine(
        metabolicEventsDao.observeInsulinApplicationsSince(historyStart().ms),
        insulinTypesMapState
    ) { entities, insulinTypesMap ->
        entities.mapNotNull { entity ->
            insulinTypesMap[entity.insulin_type_id]?.let { type -> entity.toModel(type) }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val deferredBolusesState: StateFlow<List<DeferredBolus>> = metabolicEventsDao.observeAllDeferredBoluses()
        .map { entities ->
            entities.map { it.toModel() }.sortedBy { it.timestamp }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun observeMeals(): Flow<List<MealEntry>> = mealsState

    fun observeInsulinApplications(includeCancelled: Boolean = false): Flow<List<InsulinApplication>> =
        insulinApplicationsState.map { list ->
            if (includeCancelled) list
            else list.filter { it.status != InsulinStatus.Cancelled && it.status != InsulinStatus.Invalidated }
        }

    fun observeFullMeals(): Flow<List<MealEntry>> = combine(
        metabolicEventsDao.observeAllMeals(),
        mealTypesMapState
    ) { entities, mealTypesMap ->
        entities.mapNotNull { entity ->
            mealTypesMap[entity.meal_type_id]?.let { type -> entity.toModel(type) }
        }.sortedBy { it.timestamp }
    }

    fun observeFullInsulinApplications(includeCancelled: Boolean = false): Flow<List<InsulinApplication>> = combine(
        metabolicEventsDao.observeAllInsulinApplications(),
        insulinTypesMapState
    ) { entities, insulinTypesMap ->
        entities.mapNotNull { entity ->
            insulinTypesMap[entity.insulin_type_id]?.let { type -> entity.toModel(type) }
        }.filter { includeCancelled || (it.status != InsulinStatus.Cancelled && it.status != InsulinStatus.Invalidated) }
            .sortedBy { it.timestamp }
    }

    /**
     * Returns the timestamp where the in-memory history starts.
     */
    fun historyStart(): Timestamp = Timestamp.now().minus(historySize)



    /**
     * Merges a range of insulin history from the pump.
     * Updates matching entries, inserts new entries, and cancels unconfirmed scheduled entries.
     *
     * @return The recommended [Timestamp] for subsequent syncs, or `null` if no entries.
     */
    suspend fun mergeInsulinHistory(
        history: InsulinHistory,
        insulinType: InsulinType,
        bolusDeliveryDurationPerUnit: Duration
    ): Timestamp? {
        val from = history.from
        val to = history.to

        val typesMap = insulinTypesMapState.value.ifEmpty {
            metabolicEventsDao.getAllInsulinTypes()
                .map { it.toModel() }
                .associateBy { it.id }
        }

        // Load existing pump entries in range (sorted by timestamp)
        val existingEntities = metabolicEventsDao.getInsulinApplicationsSince((from - Minutes(5)).ms)
            .filter { it.origin == InsulinOrigin.Pump && it.timestamp <= to + Minutes(5) }

        val existingApplications = existingEntities.mapNotNull { entity ->
            typesMap[entity.insulin_type_id]?.let { entity.toModel(it) }
        }

        val matchedExistingIds = mutableSetOf<Long>()
        val updatedApplications = mutableListOf<InsulinApplication>()
        val newApplications = mutableListOf<InsulinApplication>()

        // Separate history points into Basal and Bolus categories
        val basalPoints = history.points.filter { it.category == InsulinCategory.Basal && it.amount >= InsulinAmount.EPSILON }
        val bolusPoints = history.points.filter { it.category != InsulinCategory.Basal && it.amount >= InsulinAmount.EPSILON }

        // --- Phase 1: Basal Matching ---
        basalPoints.forEach { point ->
            val historyPointTimestamp = point.timestamp

            val minTime = historyPointTimestamp.minusSeconds(30)
            val maxTime = historyPointTimestamp.plusSeconds(30)

            var match: InsulinApplication? = null
            for (candidate in existingApplications) {
                if (!candidate.basal) continue
                if (matchedExistingIds.contains(candidate.id)) continue
                if (candidate.timestamp < minTime) continue
                if (candidate.timestamp > maxTime) break

                if (candidate.amount.isAlmostEqual(point.amount)) {
                    match = candidate
                    break
                }
            }

            if (match != null) {
                matchedExistingIds.add(match.id)

                if (match.status == InsulinStatus.Confirmed && match.amount.isAlmostEqual(point.amount) && match.timestamp == historyPointTimestamp) {
                    return@forEach
                }

                val newStatus = if (match.status == InsulinStatus.Invalidated) match.status else InsulinStatus.Confirmed
                val updated = match.copy(
                    dose = match.dose.copy(timestamp = historyPointTimestamp, amount = point.amount),
                    status = newStatus,
                    basal = true,
                    pumpId = point.pumpId ?: match.pumpId
                )
                updatedApplications.add(updated)
            } else {
                val newApp = InsulinApplication(
                    timestamp = historyPointTimestamp,
                    amount = point.amount,
                    insulinType = insulinType,
                    origin = InsulinOrigin.Pump,
                    basal = true,
                    status = InsulinStatus.Confirmed,
                    pumpId = point.pumpId
                )
                newApplications.add(newApp)
            }
        }

        // --- Phase 2: Bolus Matching ---
        val byPumpId = existingApplications
            .filter { !it.basal && it.pumpId != null }
            .associateBy { it.pumpId!! }

        bolusPoints.forEach { point ->
            val historyPointTimestamp = point.timestamp

            // Priority 1: O(1) lookup by pumpId
            var match: InsulinApplication? = if (point.pumpId != null) {
                val candidate = byPumpId[point.pumpId]
                if (candidate != null && !matchedExistingIds.contains(candidate.id)) candidate else null
            } else null

            // Priority 2: Confirmed entries match (timestamp ±30 seconds, matching amount)
            if (match == null) {
                val minTime = historyPointTimestamp.minusSeconds(30)
                val maxTime = historyPointTimestamp.plusSeconds(30)

                for (candidate in existingApplications) {
                    if (candidate.basal) continue
                    if (matchedExistingIds.contains(candidate.id)) continue
                    if (candidate.timestamp < minTime) continue
                    if (candidate.timestamp > maxTime) break

                    if (candidate.status == InsulinStatus.Confirmed && candidate.amount.isAlmostEqual(point.amount)) {
                        match = candidate
                        break
                    }
                }
            }

            // Priority 3: Scheduled/Cancelled entries match (start time lookback window)
            if (match == null) {
                val deliveryDuration = bolusDeliveryDurationPerUnit * point.amount.iu
                val lookbackMs = deliveryDuration.inWholeMilliseconds + 60_000L // Duration + 1 minute margin
                val minTime = historyPointTimestamp.minusMs(lookbackMs)
                val maxTime = historyPointTimestamp.plusSeconds(30)

                for (candidate in existingApplications) {
                    if (candidate.basal) continue
                    if (matchedExistingIds.contains(candidate.id)) continue
                    if (candidate.timestamp < minTime) continue
                    if (candidate.timestamp > maxTime) break

                    if ((candidate.status == InsulinStatus.Scheduled || candidate.status == InsulinStatus.Cancelled) &&
                        candidate.amount.isAlmostEqual(point.amount)
                    ) {
                        match = candidate
                        break
                    }
                }
            }

            if (match != null) {
                matchedExistingIds.add(match.id)

                if (match.status == InsulinStatus.Confirmed && match.amount.isAlmostEqual(point.amount) && match.timestamp == historyPointTimestamp) {
                    return@forEach
                }

                val newStatus = if (match.status == InsulinStatus.Invalidated) match.status else InsulinStatus.Confirmed
                val updated = match.copy(
                    dose = match.dose.copy(timestamp = historyPointTimestamp, amount = point.amount),
                    status = newStatus,
                    pumpId = point.pumpId ?: match.pumpId
                )
                updatedApplications.add(updated)
            } else {
                val newApp = InsulinApplication(
                    timestamp = historyPointTimestamp,
                    amount = point.amount,
                    insulinType = insulinType,
                    origin = InsulinOrigin.Pump,
                    status = InsulinStatus.Confirmed,
                    pumpId = point.pumpId
                )
                newApplications.add(newApp)
            }
        }

        // 3. Collect scheduled entries that were not matched to cancel (applying a 5 minute grace period)
        val graceCutoff = to.minusMinutes(5)
        val cancelledApplications = existingApplications
            .filter {
                it.status == InsulinStatus.Scheduled &&
                it.timestamp <= graceCutoff &&
                it.timestamp >= from &&
                !matchedExistingIds.contains(it.id)
            }
            .map { it.copy(status = InsulinStatus.Cancelled) }

        // 4. Perform batch DB updates and inserts
        val toUpdateEntities = (updatedApplications + cancelledApplications).map { it.toEntity() }
        if (toUpdateEntities.isNotEmpty()) {
            metabolicEventsDao.updateInsulinApplications(toUpdateEntities)
        }

        if (newApplications.isNotEmpty()) {
            val insertedIds = metabolicEventsDao.insertInsulinApplications(newApplications.map { it.toEntity() })
            newApplications.forEachIndexed { index, app ->
                if (index < insertedIds.size && insertedIds[index] != -1L) {
                    app.id = insertedIds[index]
                }
            }
        }

        // 5. Calculate next requested history start timestamp for pump
        val oldestScheduledTimestamp = insulinApplicationsState.value
            .filter { it.status == InsulinStatus.Scheduled }
            .minOfOrNull { it.timestamp }

        return oldestScheduledTimestamp?.minusMinutes(15)
    }

    /**
     * Adds a new meal entry or updates an existing one in the database.
     */
    suspend fun addMealEntry(mealEntry: MealEntry) {
        if (mealEntry.id != ID_UNDEFINED) {
            metabolicEventsDao.updateMeal(mealEntry.toEntity())
        } else {
            metabolicEventsDao.deleteMealsInRange(mealEntry.timestamp.ms, mealEntry.timestamp.ms)
            val id = metabolicEventsDao.insertMeal(mealEntry.toEntity())
            if (id != -1L) {
                mealEntry.id = id
            }
        }
    }

    /**
     * Deletes a meal entry from the database.
     */
    suspend fun removeMealEntry(mealEntry: MealEntry) {
        metabolicEventsDao.deleteMeal(mealEntry.id)
    }

    /**
     * Returns a specific meal entry by ID.
     */
    suspend fun getMeal(id: Long): MealEntry? {
        val inMemory = mealsState.value.find { it.id == id }
        if (inMemory != null) return inMemory

        val entity = metabolicEventsDao.getMealById(id) ?: return null
        val mealType = mealTypesMapState.value[entity.meal_type_id]
            ?: metabolicEventsDao.getMealTypeById(entity.meal_type_id)?.toModel()
        return mealType?.let { entity.toModel(it) }
    }

    /**
     * Adds a new insulin application or updates an existing one in the database.
     */
    suspend fun addInsulinApplication(insulinApplication: InsulinApplication) {
        if (insulinApplication.id != 0L) {
            metabolicEventsDao.updateInsulinApplication(insulinApplication.toEntity())
        } else {
            metabolicEventsDao.deleteInsulinApplicationsInRange(
                insulinApplication.timestamp.ms,
                insulinApplication.timestamp.ms,
                insulinApplication.origin
            )
            val id = metabolicEventsDao.insertInsulinApplication(insulinApplication.toEntity())
            if (id != -1L) {
                insulinApplication.id = id
            }
        }
    }

    /**
     * Updates an existing insulin application in the database.
     */
    suspend fun updateInsulinApplication(insulinApplication: InsulinApplication) {
        metabolicEventsDao.updateInsulinApplication(insulinApplication.toEntity())
    }

    /**
     * Deletes an insulin application from the database.
     */
    suspend fun removeInsulinApplication(insulinApplication: InsulinApplication) {
        metabolicEventsDao.deleteInsulinApplication(insulinApplication.id)
    }

    /**
     * Updates the status of an existing insulin application (e.g. Setting it to Invalidated or Confirmed).
     */
    suspend fun setInsulinApplicationStatus(id: Long, status: InsulinStatus): Boolean {
        val application = insulinApplicationsState.value.find { it.id == id } ?: return false
        val updated = application.copy(status = status)
        metabolicEventsDao.updateInsulinApplication(updated.toEntity())
        return true
    }

    /**
     * Returns a flattened list of meal entries within the optional timestamp range.
     */
    suspend fun getMeals(from: Timestamp? = null, to: Timestamp? = null): List<MealEntry> {
        val historyStart = historyStart()
        return mealsState.value.filter { meal ->
            meal.timestamp >= historyStart &&
            (from == null || meal.timestamp >= from) &&
            (to == null || meal.timestamp <= to)
        }
    }

    /**
     * Returns a flattened list of insulin applications within the optional timestamp range.
     */
    suspend fun getInsulinApplications(
        from: Timestamp? = null,
        to: Timestamp? = null,
        includeCancelled: Boolean = false
    ): List<InsulinApplication> {
        val historyStart = historyStart()
        return insulinApplicationsState.value.filter { insulin ->
            insulin.timestamp >= historyStart &&
            (includeCancelled || (insulin.status != InsulinStatus.Cancelled && insulin.status != InsulinStatus.Invalidated)) &&
            (from == null || insulin.timestamp >= from) &&
            (to == null || insulin.timestamp <= to)
        }
    }

    /**
     * Returns a list of scheduled (unconfirmed) insulin applications.
     */
    suspend fun getScheduledInsulinApplications(): List<InsulinApplication> {
        return insulinApplicationsState.value.filter { it.status == InsulinStatus.Scheduled }
    }

    /**
     * Returns a scheduled (unconfirmed) insulin application by its ID, if present.
     */
    suspend fun getScheduledInsulinApplication(id: Long): InsulinApplication? {
        return insulinApplicationsState.value.find { it.id == id && it.status == InsulinStatus.Scheduled }
    }

    /**
     * Updates the timestamp of a scheduled or cancelled insulin application by its ID.
     */
    suspend fun updateScheduledBolusTimestamp(
        id: Long,
        timestamp: Timestamp
    ): Boolean {
        val application = insulinApplicationsState.value.find {
            it.id == id && (it.status == InsulinStatus.Scheduled || it.status == InsulinStatus.Cancelled)
        } ?: return false

        val updated = application.copy(
            dose = application.dose.copy(timestamp = timestamp)
        )
        metabolicEventsDao.updateInsulinApplication(updated.toEntity())
        return true
    }

    /**
     * Confirms a scheduled insulin application by setting its status to Confirmed,
     * and updating its timestamp and delivered amount.
     * @return `true` if the scheduled application was found and confirmed, `false` otherwise.
     */
    suspend fun confirmScheduledBolus(
        id: Long,
        timestamp: Timestamp,
        deliveredAmount: InsulinAmount
    ): Boolean {
        val application = insulinApplicationsState.value.find {
            it.id == id && (it.status == InsulinStatus.Scheduled || it.status == InsulinStatus.Cancelled)
        } ?: return false
        val finalAmount = if (deliveredAmount > InsulinAmount.ZERO) deliveredAmount else application.dose.amount
        val finalTimestamp = if (timestamp.isValid()) timestamp else application.dose.timestamp

        val updated = application.copy(
            dose = application.dose.copy(
                timestamp = finalTimestamp,
                amount = finalAmount
            ),
            status = InsulinStatus.Confirmed
        )
        metabolicEventsDao.updateInsulinApplication(updated.toEntity())
        return true
    }

    /**
     * Cancels an insulin application by setting its status to [InsulinStatus.Cancelled].
     * @return `true` if the application was found and cancelled, `false` otherwise.
     */
    suspend fun cancelInsulinApplication(id: Long): Boolean = setInsulinApplicationStatus(id, InsulinStatus.Cancelled)

    // --- Meal Types ---

    /**
     * Observes all available meal types as a reactive stream.
     */
    fun observeMealTypes(): Flow<List<MealType>> = mealTypesMapState.map { it.values.toList() }

    /**
     * Returns all available meal types.
     */
    suspend fun getAllMealTypes(): List<MealType> {
        return mealTypesMapState.value.values.toList().ifEmpty {
            metabolicEventsDao.getAllMealTypes()
                .map { it.toModel() }
                .sortedWith(compareBy({ it.sortOrder }, { it.name }))
        }
    }

    /**
     * Inserts or updates a meal type in the database.
     */
    suspend fun insertMealType(mealType: MealType) {
        metabolicEventsDao.insertMealType(mealType.toEntity())
    }

    /**
     * Updates the sort order of the provided meal types list and persists them to the database.
     */
    suspend fun updateMealTypeOrders(orderedMealTypes: List<MealType>) {
        val updatedTypes = orderedMealTypes.mapIndexed { index, mealType ->
            mealType.copy(sortOrder = index).toEntity()
        }
        metabolicEventsDao.insertMealTypes(updatedTypes)
    }

    /**
     * Deletes a meal type from the database.
     */
    suspend fun deleteMealType(mealType: MealType) {
        metabolicEventsDao.deleteMealType(mealType.id)
    }

    // --- Insulin Types ---

    /**
     * Returns all available insulin types.
     */
    suspend fun getAllInsulinTypes(): List<InsulinType> {
        return insulinTypesMapState.value.values.toList().ifEmpty {
            metabolicEventsDao.getAllInsulinTypes()
                .map { it.toModel() }
                .sortedBy { it.name }
        }
    }

    /**
     * Observes all available insulin types as a reactive stream.
     */
    fun observeInsulinTypes(): Flow<List<InsulinType>> = insulinTypesMapState.map { it.values.toList() }

    /**
     * Observes IDs of insulin types currently referenced in insulin history.
     */
    fun observeUsedInsulinTypeIds(): Flow<Set<String>> {
        return metabolicEventsDao.observeUsedInsulinTypeIdsInHistory().map { it.toSet() }
    }

    /**
     * Checks if an insulin type is currently referenced in insulin history.
     */
    suspend fun isInsulinTypeUsedInHistory(insulinTypeId: String): Boolean {
        return metabolicEventsDao.countInsulinEntriesUsingInsulinType(insulinTypeId) > 0
    }

    /**
     * Inserts or updates an insulin type in the database.
     */
    suspend fun insertInsulinType(insulinType: InsulinType) {
        metabolicEventsDao.insertInsulinType(insulinType.toEntity())
    }

    /**
     * Deletes an insulin type from the database.
     */
    suspend fun deleteInsulinType(insulinType: InsulinType) {
        metabolicEventsDao.deleteInsulinType(insulinType.id)
    }

    // --- Deferred Boluses ---

    suspend fun addDeferredBolus(deferredBolus: DeferredBolus) {
        val id = metabolicEventsDao.insertDeferredBolus(deferredBolus.toEntity())
        if (id != -1L) {
            deferredBolus.id = id
        }
    }

    suspend fun getDeferredBoluses(): List<DeferredBolus> {
        return deferredBolusesState.value
    }

    suspend fun purgeExpiredDeferredBoluses(now: Timestamp = Timestamp.now()) {
        val expired = deferredBolusesState.value.filter { deferredBolus ->
            deferredBolus.timestamp < now &&
                Minutes.timeDifference(deferredBolus.timestamp, now) > STALE_DEFERRED_BOLUS_THRESHOLD
        }

        if (expired.isNotEmpty()) {
            val idsToRemove = expired.map { it.id }.filter { it != ID_UNDEFINED }
            if (idsToRemove.isNotEmpty()) {
                metabolicEventsDao.deleteDeferredBoluses(idsToRemove)
            }
        }
    }

    suspend fun updateDeferredBolus(deferredBolus: DeferredBolus) {
        metabolicEventsDao.updateDeferredBolus(deferredBolus.toEntity())
    }

    suspend fun removeDeferredBolus(deferredBolus: DeferredBolus) {
        metabolicEventsDao.deleteDeferredBolus(deferredBolus.id)
    }

    suspend fun removeDeferredBoluses(deferredBolusesToRemove: Collection<DeferredBolus>) {
        val idsToRemove = deferredBolusesToRemove.map { it.id }.filter { it != ID_UNDEFINED }
        if (idsToRemove.isEmpty()) return
        metabolicEventsDao.deleteDeferredBoluses(idsToRemove)
    }

    suspend fun addScheduledPumpInsulinEntry(
        timestamp: Timestamp,
        amount: InsulinAmount,
        insulinType: InsulinType,
        basal: Boolean,
        correction: Boolean,
        meal: Boolean
    ): InsulinApplication {
        val application = InsulinApplication(
            timestamp = timestamp,
            amount = amount,
            insulinType = insulinType,
            origin = InsulinOrigin.Pump,
            basal = basal,
            correction = correction,
            meal = meal,
            status = InsulinStatus.Scheduled
        )
        addInsulinApplication(application)
        return application
    }

    suspend fun addAdministeredInsulinToMeal(mealId: Long, amount: InsulinAmount) {
        if (amount <= InsulinAmount.ZERO) return
        metabolicEventsDao.addAdministeredInsulinToMeal(mealId, amount.iu)
    }
}