package com.cmu.a75hard.model.DayData

import androidx.room.*

@Dao
interface DayDataDao {

    // Insere ou substitui um registro de DayData
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayData(dayData: DayData)

    // Atualiza um registro de DayData
    @Update
    suspend fun updateDayData(dayData: DayData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDayData(dayData: DayData)

    // Obtém um DayData específico pelo número do dia
    @Query("SELECT * FROM day_data WHERE dayNumber = :dayNumber")
    suspend fun getDayData(dayNumber: Int): DayData?

    // Obtém todos os registros de DayData
    @Query("SELECT * FROM day_data")
    suspend fun getAllDaysData(): List<DayData>

    // Atualiza o status de conclusão para um dia específico
    @Query("UPDATE day_data SET isCompleted = :isCompleted WHERE dayNumber = :dayNumber")
    suspend fun updateDayCompletionStatus(dayNumber: Int, isCompleted: Boolean)

    // Limpa todos os registros de DayData
    @Query("DELETE FROM day_data")
    suspend fun clearAllData()

    // Obtém uma lista de dias com URLs de fotos de progresso para todos os utilizadores
    @Query("SELECT dayNumber, progressPictureUrl FROM day_data WHERE progressPictureUrl IS NOT NULL")
    suspend fun getAllProgressUrls(): List<ProgressImage>

    // Obtém uma lista de dias com URLs de fotos de progresso para um utilizador específico
    @Query("SELECT dayNumber, progressPictureUrl FROM day_data WHERE userId = :userId AND progressPictureUrl IS NOT NULL ORDER BY dayNumber")
    suspend fun getProgressUrlsForUser(userId: Int): List<ProgressImage>

    @Query("SELECT weight FROM day_data WHERE dayNumber = :dayNumber")
    suspend fun getWeightForDay(dayNumber: Int): Float?

    @Query("UPDATE day_data SET weight = :weight WHERE dayNumber = :dayNumber")
    suspend fun updateWeightForDay(dayNumber: Int, weight: Float)

    @Query("SELECT * FROM day_data WHERE dayNumber = :dayNumber AND userId = :userId")
    suspend fun getDayDataForUser(dayNumber: Int, userId: Int): DayData?

    @Query("SELECT * FROM day_data WHERE userId = :userId")
    fun getAllDaysDataForUser(userId: Int): List<DayData>

    @Query("SELECT MAX(dayNumber) FROM day_data WHERE userId = :userId AND isCompleted = 1")
    suspend fun getLastCompletedDay(userId: Int): Int?


    // Classe de suporte para armazenar dia e URL
    data class ProgressImage(
        val dayNumber: Int,
        val progressPictureUrl: String
    )

}
