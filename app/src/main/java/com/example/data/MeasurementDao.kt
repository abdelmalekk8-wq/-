package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_records ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(record: MeasurementRecord)

    @Query("DELETE FROM measurement_records WHERE id = :id")
    suspend fun deleteMeasurementById(id: Int)

    @Query("DELETE FROM measurement_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM measurement_records")
    suspend fun count(): Int
}
