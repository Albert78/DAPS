package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.core.repository.db.entities.DeferredBolusEntity
import de.dh.daps.core.repository.db.entities.InsulinEntity
import de.dh.daps.core.repository.db.entities.InsulinTypeEntity
import de.dh.daps.core.repository.db.entities.MealEntity
import de.dh.daps.core.repository.db.entities.MealTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetabolicEventsDao {
    // Meal Types
    @Query("SELECT * FROM meal_type ORDER BY sort_order ASC, name ASC")
    suspend fun getAllMealTypes(): List<MealTypeEntity>

    @Query("SELECT * FROM meal_type ORDER BY sort_order ASC, name ASC")
    fun observeAllMealTypes(): Flow<List<MealTypeEntity>>

    @Query("SELECT * FROM meal_type WHERE id = :id")
    suspend fun getMealTypeById(id: String): MealTypeEntity?

    @Upsert
    suspend fun insertMealType(mealType: MealTypeEntity)

    @Update
    suspend fun updateMealType(mealType: MealTypeEntity)

    @Query("DELETE FROM meal_type where id = :mealTypeId")
    suspend fun deleteMealType(mealTypeId: String)

    @Query("DELETE FROM meal_type")
    suspend fun deleteAllMealTypes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealTypes(items: List<MealTypeEntity>)

    // Meals
    @Query("SELECT * FROM meal ORDER BY timestamp ASC")
    suspend fun getAllMeals(): List<MealEntity>

    @Query("SELECT * FROM meal ORDER BY timestamp ASC")
    fun observeAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getMealsSince(since: Long): List<MealEntity>

    @Query("SELECT * FROM meal WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?

    @Insert
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("UPDATE meal SET administeredInsulinAmount = administeredInsulinAmount + :amount WHERE id = :mealId")
    suspend fun addAdministeredInsulinToMeal(mealId: Long, amount: Double)

    @Query("UPDATE meal SET administeredInsulinAmount = :amount WHERE id = :mealId")
    suspend fun setAdministeredInsulinAmount(mealId: Long, amount: Double)

    @Query("DELETE FROM meal where id = :mealId")
    suspend fun deleteMeal(mealId: Long)

    @Query("DELETE FROM meal")
    suspend fun deleteAllMeals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(items: List<MealEntity>)

    // Insulin Types
    @Query("SELECT * FROM insulin_type ORDER BY name ASC")
    suspend fun getAllInsulinTypes(): List<InsulinTypeEntity>

    @Query("SELECT * FROM insulin_type ORDER BY name ASC")
    fun observeAllInsulinTypes(): Flow<List<InsulinTypeEntity>>

    @Query("SELECT * FROM insulin_type WHERE id = :id")
    suspend fun getInsulinTypeById(id: String): InsulinTypeEntity?

    @Query("SELECT * FROM insulin_type WHERE name = :name")
    suspend fun getInsulinTypeByName(name: String): InsulinTypeEntity?

    @Upsert
    suspend fun insertInsulinType(insulinType: InsulinTypeEntity)

    @Update
    suspend fun updateInsulinType(insulinType: InsulinTypeEntity)

    @Query("DELETE FROM INSULIN_TYPE where id = :insulinTypeId")
    suspend fun deleteInsulinType(insulinTypeId: String)

    @Query("DELETE FROM insulin_type")
    suspend fun deleteAllInsulinTypes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsulinTypes(items: List<InsulinTypeEntity>)

    // Insulin
    @Query("SELECT * FROM insulin ORDER BY timestamp ASC")
    suspend fun getAllInsulinApplications(): List<InsulinEntity>

    @Query("SELECT * FROM insulin ORDER BY timestamp ASC")
    fun observeAllInsulinApplications(): Flow<List<InsulinEntity>>

    @Query("SELECT * FROM insulin WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getInsulinApplicationsSince(since: Long): List<InsulinEntity>

    @Insert
    suspend fun insertInsulinApplication(insulin: InsulinEntity): Long

    @Insert
    suspend fun insertInsulinApplications(insulin: List<InsulinEntity>): List<Long>

    @Update
    suspend fun updateInsulinApplication(insulin: InsulinEntity)

    @Update
    suspend fun updateInsulinApplications(insulin: List<InsulinEntity>)

    @Query("DELETE FROM insulin where id = :id")
    suspend fun deleteInsulinApplication(id: Long)

    @Query("DELETE FROM insulin")
    suspend fun deleteAllInsulinApplications()

    @Query("DELETE FROM insulin WHERE origin = :origin AND timestamp >= :from AND timestamp <= :to")
    suspend fun deleteInsulinApplicationsInRange(from: Long, to: Long, origin: InsulinOrigin)

    @Transaction
    suspend fun replaceInsulinApplicationsInRange(from: Long, to: Long, origin: InsulinOrigin, newApplications: List<InsulinEntity>) {
        deleteInsulinApplicationsInRange(from, to, origin)
        insertInsulinApplications(newApplications)
    }

    @Query("DELETE FROM meal WHERE timestamp >= :from AND timestamp <= :to")
    suspend fun deleteMealsInRange(from: Long, to: Long)

    // Deferred Bolus
    @Query("SELECT * FROM deferred_bolus ORDER BY timestamp ASC")
    suspend fun getAllDeferredBoluses(): List<DeferredBolusEntity>

    @Insert
    suspend fun insertDeferredBolus(deferredBolus: DeferredBolusEntity): Long

    @Update
    suspend fun updateDeferredBolus(deferredBolus: DeferredBolusEntity)

    @Query("DELETE FROM deferred_bolus WHERE id = :id")
    suspend fun deleteDeferredBolus(id: Long)

    @Query("DELETE FROM deferred_bolus WHERE id IN (:ids)")
    suspend fun deleteDeferredBoluses(ids: List<Long>)

    @Query("DELETE FROM deferred_bolus")
    suspend fun deleteAllDeferredBoluses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeferredBoluses(items: List<DeferredBolusEntity>)
}