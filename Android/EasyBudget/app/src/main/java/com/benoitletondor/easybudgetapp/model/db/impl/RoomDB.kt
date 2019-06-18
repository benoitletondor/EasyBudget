package com.benoitletondor.easybudgetapp.model.db.impl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benoitletondor.easybudgetapp.model.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.model.db.impl.entity.RecurringExpenseEntity
import java.util.*

@Database(exportSchema = false,
          version = 2,
          entities = [
              ExpenseEntity::class,
              RecurringExpenseEntity::class
          ])
@TypeConverters(TimestampConverters::class)
abstract class RoomDB : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        fun create(context: Context): RoomDB = Room
            .databaseBuilder(context, RoomDB::class.java, "easybudget.db")
            .addMigrations(initialMigration)
            .build()
    }
}

private class TimestampConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

private val initialMigration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op
    }
}
