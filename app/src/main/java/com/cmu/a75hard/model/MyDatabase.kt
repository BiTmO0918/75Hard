package com.cmu.a75hard.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.DayData.DayDataDao
import com.cmu.a75hard.model.user.UserDao
import com.cmu.a75hard.model.user.User

@Database(
    entities = [DayData::class, User::class],
    version = 14,
    exportSchema = false
)
abstract class MyDatabase : RoomDatabase() {

    abstract fun dayDataDao(): DayDataDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MyDatabase? = null

        fun getDatabase(context: Context): MyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyDatabase::class.java,
                    "day_data_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
