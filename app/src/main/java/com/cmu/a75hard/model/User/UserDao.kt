package com.cmu.a75hard.model.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.cmu.a75hard.model.UserWithDayData

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM user_table WHERE userId = :id")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM User_table WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @androidx.room.Transaction
    @Query("SELECT * FROM User_table WHERE userId = :userId")
    suspend fun getUserWithDayData(userId: Int): UserWithDayData?

    @Query("SELECT email FROM User_table WHERE userId = :userId LIMIT 1")
    suspend fun getEmailByUserId(userId: Int): String?

}
