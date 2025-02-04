package com.cmu.a75hard.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cmu.a75hard.model.DayData.DayDataDao

class OutdoorWorkoutViewModelFactory(
    private val dayDataDao: DayDataDao,
    private val userId: Int,
    private val dayNumber: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OutdoorWorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OutdoorWorkoutViewModel(dayDataDao, userId, dayNumber) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
