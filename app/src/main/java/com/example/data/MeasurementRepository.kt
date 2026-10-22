package com.example.data

import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {
    val allMeasurements: Flow<List<MeasurementRecord>> = dao.getAllMeasurements()

    suspend fun insert(record: MeasurementRecord) {
        dao.insertMeasurement(record)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteMeasurementById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun seedInitialExamplesIfEmpty() {
        if (dao.count() == 0) {
            dao.insertMeasurement(
                MeasurementRecord(
                    title = "حاسوب محمول 14 بوصة (عرض الهيكل)",
                    primaryLengthCm = 30.41,
                    secondaryLengthCm = 21.24,
                    calibrationMethod = "معايرة بمرجع في الصورة",
                    referenceDetails = "بطاقة قياسية ISO 7810 (8.56 سم) • ميل 1.2°",
                    confidencePercent = 98,
                    notes = "تم القياس عبر استوديو الصور مع العدسة المكبرة الدقيقة"
                )
            )
            dao.insertMeasurement(
                MeasurementRecord(
                    title = "إطار باب الغرفة الخشبي",
                    primaryLengthCm = 91.50,
                    secondaryLengthCm = 208.00,
                    calibrationMethod = "تثليث زاوية الميل والارتفاع",
                    referenceDetails = "ارتفاع الكاميرا 150 سم • المسافة الأفقية 240 سم",
                    confidencePercent = 94,
                    notes = "قياس عن بُعد باستخدام حساس الميل البصري"
                )
            )
        }
    }
}
