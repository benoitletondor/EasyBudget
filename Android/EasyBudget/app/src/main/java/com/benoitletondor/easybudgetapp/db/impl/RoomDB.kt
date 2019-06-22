package com.benoitletondor.easybudgetapp.db.impl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benoitletondor.easybudgetapp.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.impl.entity.RecurringExpenseEntity
import java.util.*

@Database(exportSchema = false,
          version = 4,
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
            .addMigrations(initialMigrationToRoom)
            .build()
    }
}

private class TimestampConverters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

private val initialMigrationToRoom = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op, simple migration from SQLite to Room
    }
}
