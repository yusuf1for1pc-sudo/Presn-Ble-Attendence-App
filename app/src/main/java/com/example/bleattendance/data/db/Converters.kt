// In data/db/Converters.kt
package com.example.bleattendance.data.db

import androidx.room.TypeConverter
import com.example.bleattendance.model.StudentInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Tells Room how to convert complex types (like a list of students) into a format it can store (JSON string)
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStudentInfoList(studentList: List<StudentInfo>?): String {
        return gson.toJson(studentList)
    }

    @TypeConverter
    fun toStudentInfoList(studentListJson: String?): List<StudentInfo> {
        if (studentListJson == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<StudentInfo>>() {}.type
        return gson.fromJson(studentListJson, listType)
    }
}
