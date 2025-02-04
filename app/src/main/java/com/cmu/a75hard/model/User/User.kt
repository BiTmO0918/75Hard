package com.cmu.a75hard.model.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "User_table")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val address: String,
    val city: String,
    val password: String,
    val height: Int,
    val weightLost: Float = 0.0F // Valor quando registado
)
