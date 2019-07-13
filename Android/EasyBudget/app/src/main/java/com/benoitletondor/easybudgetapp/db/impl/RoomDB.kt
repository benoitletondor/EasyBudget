/*
 *   Copyright 2019 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.db.impl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benoitletondor.easybudgetapp.db.impl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.impl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
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
            .addMigrations(migrationFrom1To2, migrationFrom2To3, migrationToRoom)
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

private val migrationToRoom = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No-op, simple migration from SQLite to Room
    }
}

private val migrationFrom2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE monthlyexpense ADD COLUMN type text not null DEFAULT '"+ RecurringExpenseType.MONTHLY+"'")
    }
}

private val migrationFrom1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE expense SET amount = amount * 100")
        database.execSQL("UPDATE monthlyexpense SET amount = amount * 100")
    }
}
