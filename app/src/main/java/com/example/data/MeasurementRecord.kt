package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_records")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val primaryLengthCm: Double,
    val secondaryLengthCm: Double? = null,
    val calibrationMethod: String,
    val referenceDetails: String,
    val confidencePercent: Int,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
