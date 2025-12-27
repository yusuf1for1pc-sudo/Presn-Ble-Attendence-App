// In data/db/AppDatabase.kt
package com.example.bleattendance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherEntity::class, 
        StudentEntity::class, 
        ClassEntity::class, 
        AttendanceSessionEntity::class,
        AcademicConfigEntity::class,
        StudentGroupEntity::class,
        ClassSessionEntity::class,
        AssignmentEntity::class,
        SubmissionEntity::class
    ],
    version = 9, // Increment version for password fields added to TeacherEntity and StudentEntity
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun teacherDao(): TeacherDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun academicConfigDao(): AcademicConfigDao
    abstract fun studentGroupDao(): StudentGroupDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun submissionDao(): SubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ble_attendance_database"
                )
                    // Clear database on version change to handle new entities
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Temporary for debugging
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Database created successfully
                            println("Database created successfully")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Database opened successfully
                            println("Database opened successfully")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
