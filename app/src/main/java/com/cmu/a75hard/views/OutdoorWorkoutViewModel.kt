package com.cmu.a75hard.views

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.DayData.WorkoutData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OutdoorWorkoutViewModel(
    private val dayDataDao: DayDataDao,
    private val userId: Int,
    private val dayNumber: Int
) : ViewModel() {

    // Propriedades privadas mutáveis
    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> get() = _isRunning

    private val _timerText = mutableStateOf("00:00:00")
    val timerText: State<String> get() = _timerText

    private val _elapsedMillis = mutableStateOf(0L)
    val elapsedMillis: State<Long> get() = _elapsedMillis

    private val _distance = mutableStateOf(0.0)
    val distance: State<Double> get() = _distance

    private val _pace = mutableStateOf("00:00")
    val pace: State<String> get() = _pace

    private val _calories = mutableStateOf(0)
    val calories: State<Int> get() = _calories

    private val _maxSpeed = mutableStateOf(0.0)
    val maxSpeed: State<Double> get() = _maxSpeed

    private val _heartRate = mutableStateOf(0)
    val heartRate: State<Int> get() = _heartRate

    private val _acceleration = mutableStateOf(0.0)
    val acceleration: State<Double> get() = _acceleration

    private val _stepCount = mutableStateOf(0)
    val stepCount: State<Int> get() = _stepCount


    private var _lastAcceleration = 0.0

    fun getLastAcceleration(): Double {
        return _lastAcceleration
    }

    fun setLastAcceleration(value: Double) {
        _lastAcceleration = value
    }


    fun updateStepCount(newStepCount: Int) {
        _stepCount.value = newStepCount
    }

    val path = mutableStateListOf<LatLng>()

    private val heartRateList = mutableStateListOf<Int>()
    private val accelerationList = mutableStateListOf<Double>()

    private var startTime: Long = 0L

    // Iniciar o treino
    fun startWorkout() {
        if (!_isRunning.value) {
            _isRunning.value = true
            startTime = System.currentTimeMillis() - _elapsedMillis.value
            viewModelScope.launch {
                while (_isRunning.value) {
                    _elapsedMillis.value = System.currentTimeMillis() - startTime
                    _timerText.value = calculateTimeElapsed(startTime)
                    _pace.value = if (_distance.value > 0) calculatePace(_elapsedMillis.value, _distance.value) else "00:00"
                    delay(1000L)
                }
            }
        }
    }

    // Parar o treino
    fun stopWorkout() {
        if (_isRunning.value) {
            _isRunning.value = false
            viewModelScope.launch {
                saveWorkoutData()
            }
        }
    }

    // Adicionar nova localização
    fun addLocation(newLocation: LatLng, speed: Float?) {
        path.add(newLocation)
        if (_isRunning.value && path.size > 1) {
            val previousLocation = path[path.size - 2]
            val distanceBetween = calculateDistance(previousLocation, newLocation)
            _distance.value += distanceBetween
            if (speed != null) {
                _maxSpeed.value = maxOf(_maxSpeed.value, speed.toDouble())
            }
            _calories.value = (_distance.value * 60).toInt()
        }
    }

    // Adicionar nova frequência cardíaca
    fun addHeartRate(newHeartRate: Int) {
        if (_isRunning.value) {
            _heartRate.value = newHeartRate
            heartRateList.add(newHeartRate)
        }
    }

    // Adicionar nova aceleração
    fun addAcceleration(newAcceleration: Double) {
        if (_isRunning.value) {
            _acceleration.value = newAcceleration
            accelerationList.add(newAcceleration)
        }
    }

    // Definir frequência cardíaca como indisponível
    fun setHeartRateUnavailable() {
        _heartRate.value = -1
    }

    // Salvar dados do treino no banco de dados
    private suspend fun saveWorkoutData() {
        val averageHeartRate = if (heartRateList.isNotEmpty()) heartRateList.average().toInt() else 0
        val maxAcceleration = accelerationList.maxOrNull() ?: 0.0

        val workoutData = WorkoutData(
            duration = calculateTimeElapsed(_elapsedMillis.value),
            maxSpeed = _maxSpeed.value,
            pace = _pace.value,
            caloriesBurned = _calories.value,
            distance = _distance.value,
            averageHeartRate = averageHeartRate,
            maxAcceleration = maxAcceleration,
            steps = _stepCount.value // Salvar os passos registrados
        )

        val dayData = dayDataDao.getDayDataForUser(dayNumber, userId) ?: DayData(
            dayNumber = dayNumber,
            userId = userId
        )

        val updatedDayData = dayData.copy(outdoorWorkout = workoutData)
        dayDataDao.insertDayData(updatedDayData)
    }


    // Funções auxiliares
    private fun calculateTimeElapsed(startTime: Long): String {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / (1000 * 60) % 60)
        val hours = (elapsedMillis / (1000 * 60 * 60) % 24)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] / 1000 // Retorna em KM
    }

    private fun calculatePace(elapsedMillis: Long, distance: Double): String {
        val paceInSeconds = (elapsedMillis / 1000.0) / distance
        val minutes = (paceInSeconds / 60).toInt()
        val seconds = (paceInSeconds % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }
}
