package com.cmu.a75hard.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cmu.a75hard.model.DayData.DayData
import com.cmu.a75hard.model.user.User

data class UserWithDayData(
    @Embedded val user: User,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val dayDataList: List<DayData>
)
