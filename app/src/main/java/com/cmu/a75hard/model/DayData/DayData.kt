package com.cmu.a75hard.model.DayData

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ForeignKey
import com.cmu.a75hard.model.user.User

@Entity(
    tableName = "day_data",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // se o user for eliminado, os seus DayData tbm sao eliminados.
        )
    ]
)
data class DayData(
    @PrimaryKey val dayNumber: Int,
    val userId: Int, // Chave estrangeira referencia o utilizador
    val isCompleted: Boolean = false,
    @Embedded val outdoorWorkout: WorkoutData? = null,
    @Embedded(prefix = "indoor_") val indoorWorkout: WorkoutData? = null,
    val diet: Boolean = false,
    val reading: Boolean = false,
    val waterIntake: Double = 0.0,
    val progressPictureUrl: String? = null,
    val noAlcohol: Boolean = false,
    val weight: Float? = null
)

data class WorkoutData(
    val duration: String,         // Duração do treino (exemplo: "00:45:30")
    val maxSpeed: Double,         // Velocidade máxima registrada (em m/s ou km/h)
    val pace: String,             // Ritmo médio (exemplo: "05:30 min/km")
    val caloriesBurned: Int,      // Calorias queimadas
    val distance: Double,         // Distância percorrida (em KM)
    val averageHeartRate: Int,    // Frequência cardíaca média (em BPM)
    val maxAcceleration: Double,  // Aceleração máxima registrada (em m/s²)
    val steps: Int                // Número de passos registrados
)

