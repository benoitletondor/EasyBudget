/*
 *   Copyright 2024 Benoit LETONDOR
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

package com.benoitletondor.easybudgetapp.db.offlineimpl

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.ExpenseEntity
import com.benoitletondor.easybudgetapp.db.offlineimpl.entity.RecurringExpenseEntity
import com.benoitletondor.easybudgetapp.helper.localDateFromTimestamp
import com.benoitletondor.easybudgetapp.model.RecurringExpenseType
import java.time.LocalDate

const val DB_NAME = "easybudget.db"

@Database(exportSchema = false,
          version = 6,
          entities = [
              ExpenseEntity::class,
              RecurringExpenseEntity::class
          ])
@TypeConverters(TimestampConverters::class)
abstract class RoomDB : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        fun create(context: Context): RoomDB = Room
            .databaseBuilder(context, RoomDB::class.java, DB_NAME)
            .addMigrations(migrationFrom1To2, migrationFrom2To3, migrationToRoom, addCheckedField, migrateTimestamps)
            .build()
    }
}

private class TimestampConverters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}

private val migrateTimestamps = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("SELECT _expense_id,date FROM expense")
        while(cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_expense_id"))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            db.execSQL("UPDATE expense SET date = $newTimestamp WHERE _expense_id = $id")
        }

        val cursorRecurring = db.query("SELECT _expense_id,recurringDate FROM monthlyexpense")
        while(cursorRecurring.moveToNext()) {
            val id = cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("_expense_id"))
            val timestamp = cursorRecurring.getLong(cursorRecurring.getColumnIndexOrThrow("recurringDate"))

            val localDate = localDateFromTimestamp(timestamp)
            val newTimestamp = localDate.toEpochDay()

            db.execSQL("UPDATE monthlyexpense SET recurringDate = $newTimestamp WHERE _expense_id = $id")
        }
    }
}

private val addCheckedField = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense ADD COLUMN checked INTEGER NOT NULL DEFAULT 0")
    }
}

private val migrationToRoom = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op, simple migration from SQLite to Room
    }
}

private val migrationFrom2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE monthlyexpense ADD COLUMN type text not null DEFAULT '"+ RecurringExpenseType.MONTHLY+"'")
    }
}

private val migrationFrom1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE expense SET amount = amount * 100")
        db.execSQL("UPDATE monthlyexpense SET amount = amount * 100")
    }
}
